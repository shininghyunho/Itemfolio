import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.math.ceil
import kotlin.math.min

// 도감 map <아이템 영문명,해금한 유저 이름>
private val dogam = mutableMapOf<String,String>()
// 경품권 유저 map <유저 이름, 경품권 수>
private val giftUserMap = mutableMapOf<String,Int>()
// 타켓 아이템 map <아이템 영문명, 아이템 갯수>
private val targetMap = mutableMapOf<String,Int>()
// 아이템 한글 이름 map <아이템 영문명, 아이템 한글명>
private val materialKorMap = mutableMapOf<String,String>()

class Main : JavaPlugin(), Listener, CommandExecutor {
    // 플러그인 활성화시
    override fun onEnable() {
        logger.info("[Itemfolio] 활성화 되었습니다!")
        // 이벤트 등록
        server.pluginManager.registerEvents(this, this)
        // 커맨드 등록
        getCommand("itemfolio")?.setExecutor(this) ?: logger.warning("itemfolio command is not registered!")
        
        // 도감, 경품권 유저 로드
        load()
    }

    // 플러그인 비활성화시
    override fun onDisable() {
        logger.info("[Itemfolio] 비활성화 되었습니다!")
        // 도감, 경품권 유저 저장
        save()
    }

    // 유저가 나가면 저장
    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        save()
    }

    // 커맨드 실행시
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        // not instanceof Player then return
        if (sender !is Player) return false

        // 명령어 안내
        if(label == "help" || label == "?"){
            sender.sendPlainMessage("${ChatColor.GRAY} 명령어 안내입니다.")
            // 해금
            sender.sendPlainMessage("${ChatColor.AQUA} /해금,/unlock,/ul <숫자> ${ChatColor.GRAY}: 아이템을 해금합니다.")
            // 타겟
            sender.sendPlainMessage("${ChatColor.AQUA} /타겟,/target,/t <숫자> ${ChatColor.GRAY}: 타겟 아이템을 확인합니다.")
            // 진행도
            sender.sendPlainMessage("${ChatColor.AQUA} /진행도,/진행,/진행률,/progress,/p ${ChatColor.GRAY}: 도감 진행률을 확인합니다.")
            // 도감
            sender.sendPlainMessage("${ChatColor.AQUA} /도감,/dogam,/d <숫자> ${ChatColor.GRAY}: 도감을 확인합니다.")
            // 순위
            sender.sendPlainMessage("${ChatColor.AQUA} /순위,/rank ${ChatColor.GRAY}: 도감 순위를 확인합니다.")
            // 경품권
            sender.sendPlainMessage("${ChatColor.AQUA} /경품권,/gift,/g ${ChatColor.GRAY}: 경품권을 확인합니다.")
            // 경품권 사용
            sender.sendPlainMessage("${ChatColor.AQUA} /경품권사용,/usegift,/ug ${ChatColor.GRAY}: 경품권을 사용합니다.")
        }
        // 해금 입력시 /해금 <아이템 갯수>
        if(label in listOf("해금","unlock","ul")) {
            // 해금만 입력시 안내문구
            if(args == null || args.isEmpty() || args[0].toIntOrNull() == null) {
                sender.sendPlainMessage("${ChatColor.AQUA} /해금,/unlock,/ul <아이템 갯수> ${ChatColor.GRAY}: 아이템을 몇개 해금할지 입력해주세요.")
                return true
            }

            // sender 가 들고있는 아이템
            val itemStack = sender.inventory.itemInMainHand
            val itemName = itemStack.type.name
            val korItemName = getKorName(itemName)
            // 아이템이 targetMap 에 없으면 return
            if(!targetMap.containsKey(itemName)) {
                sender.sendPlainMessage("${ChatColor.GOLD} $korItemName ${ChatColor.GRAY}은(는) 해금할 수 없는 아이템입니다. ㅠㅠ")
                return true
            }
            // 아이템 해금
            val unlockCnt = args[0].toInt()
            val isUnlock = unlockTarget(sender.name,itemName,unlockCnt)
            if(!isUnlock) {
                // target 에 남은 아이템 갯수 전달
                val targetCnt = targetMap[itemName] ?: 0
                sender.sendPlainMessage("${ChatColor.GOLD}${korItemName} ${ChatColor.GRAY}은(는) ${ChatColor.GOLD}${targetCnt}개 남았습니다.")
            }
        }
        // 타켓 입력시 /타켓 <숫자>
        if(label in listOf("타켓","target","t")) {
            // 타켓 입력시 안내문구
            if(args == null || args.isEmpty() || args[0].toIntOrNull() == null) {
                sender.sendPlainMessage("${ChatColor.AQUA} /타겟, /target, /t <숫자> ${ChatColor.GRAY}: 타켓의 몇번째 페이지를 보실지 입력해주세요.")
                return true
            }

            // 페이지 번호
            val pageCnt = 20 // 한페이지에 보여줄 갯수
            val page = args[0].toInt()
            val totalPage = ceil(targetMap.size.toDouble() / pageCnt).toInt()
            // 페이지 범위 체크
            if(totalPage == 0) {
                sender.sendPlainMessage("${ChatColor.RED} 헉 타켓이 비어있어요. target.yml 파일을 확인해주세요.")
                return true
            }
            if(page < 1 || page > totalPage) {
                sender.sendPlainMessage("${ChatColor.GRAY} 1~${totalPage} 페이지 사이의 숫자를 입력해주세요.")
                return true
            }

            // target map 형태로 되어있으므로 아이템 한글명 기준으로 정렬
            val targetList = mutableListOf<Pair<String,Int>>()
            targetMap.forEach { (itemName, cnt) -> targetList.add(Pair(getKorName(itemName),cnt)) }
            // 페이지에 맞는 타겟을 가져옴
            val targetPage = targetList.subList((page-1)*pageCnt, min(page*pageCnt,targetList.size))
            // 한줄에 5개씩 보여줌
            val targetLine = 5
            for(i in targetPage.indices step targetLine) {
                val targetLineList = targetPage.subList(i,min(i+targetLine,targetPage.size))
                val targetLineStr = targetLineList.joinToString("${ChatColor.GRAY}, ") { "${ChatColor.GOLD}${it.first}:${it.second}" }
                sender.sendPlainMessage("${ChatColor.GRAY} ${i+1}~${i+targetLineList.size} : $targetLineStr")
            }
            // 페이지 정보
            sender.sendPlainMessage("${ChatColor.GRAY}현재/전체 ${ChatColor.GOLD}${page}/${totalPage}")

        }
        // 도감 입력시
        if(label == "도감" || label == "dogam" || label == "d") {
            // 도감만 입력시 안내문구
            if(args == null || args.isEmpty() || args[0].toIntOrNull() == null) {
                sender.sendPlainMessage("${ChatColor.AQUA} /도감,/dogam,/d <숫자> ${ChatColor.GRAY}: 도감의 몇번째 페이지를 보실지 입력해주세요.")
                return true
            }

            // 페이지 번호
            val pageCnt = 20 // 한페이지에 보여줄 갯수
            val page = args[0].toInt()
            val totalPage = ceil(dogam.size.toDouble() / pageCnt).toInt()
            // 페이지 범위 체크
            if(totalPage == 0) {
                sender.sendPlainMessage("${ChatColor.GRAY} 아직 아무것도 해금하지 않으셨습니다.")
                return true
            }
            if(page < 1 || page > totalPage) {
                sender.sendPlainMessage("${ChatColor.AQUA} /도감 <숫자> ${ChatColor.GRAY}: 도감의 범위를 벗어났습니다. 1~${totalPage}까지 입력해주세요.")
                return true
            }

            // 도감이 map 형태로 되어있으므로 아이템 한글명 기준으로 정렬
            val dogamList = mutableListOf<String>()
            dogam.forEach { (itemName, cnt) -> dogamList.add(getKorName(itemName)) }
            // 페이지에 맞는 도감을 가져옴
            val dogamPage = dogamList.subList((page-1)*pageCnt, min(page*pageCnt,dogamList.size))
            // 한줄에 5개씩 보여줌
            val dogamLine = 5
            for(i in dogamPage.indices step dogamLine) {
                val dogamLineList = dogamPage.subList(i,Math.min(i+dogamLine,dogamPage.size))
                val dogamLineStr = dogamLineList.joinToString(", ") { it }
                sender.sendPlainMessage("${ChatColor.GOLD}$dogamLineStr")
            }
            sender.sendPlainMessage("${ChatColor.GRAY}현재/전체 : ${ChatColor.GOLD}${page}/${totalPage}")
            return true
        }

        // 순위 입력시
        if(label == "순위" || label == "rank") {
            // 플레이어의 해금 갯수를 순위별로 알려줌
            val userDogamCntMap = mutableMapOf<String,Int>()
            dogam.forEach { (itemName,userName) ->
                if(userDogamCntMap.containsKey(userName)) {
                    userDogamCntMap[userName] = userDogamCntMap[userName]!! + 1
                } else {
                    userDogamCntMap[userName] = 1
                }
            }

            // 순위별로 정렬
            val userRankList : ArrayList<Pair<String,Int>> = ArrayList()
            userDogamCntMap.forEach { (userName,dogamCnt) ->
                userRankList.add(Pair(userName,dogamCnt))
            }
            userRankList.sortByDescending { it.second }

            // 메시지
            for(i in 0 until userRankList.size) {
                val (userName,dogamCnt) = userRankList[i]
                val message = "${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}${i+1}위: ${ChatColor.GOLD}$userName${ChatColor.GRAY}(${ChatColor.GOLD}$dogamCnt${ChatColor.GRAY}개)"
                sender.sendPlainMessage(message)
            }
            return true
        }

        // 경품권 입력시
        if(label == "경품권" || label == "gift" || label == "g") {
            val userName = sender.name
            val giftCnt = giftUserMap[userName] ?: 0
            val message = "${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}경품권 갯수: ${ChatColor.GOLD}$giftCnt${ChatColor.GRAY}개\n"+
                    "${ChatColor.GREEN}[알 림] ${ChatColor.AQUA}/경품권사용,/usegift,/ug ${ChatColor.GRAY}입력시 경품권을 사용할 수 있습니다~!"
            sender.sendPlainMessage(message)
            return true
        }

        // 경품권사용 입력시
        if(label == "경품권사용" || label == "usegift" || label == "ug") {
            val userName = sender.name
            val giftCnt = giftUserMap[userName] ?: 0
            if(giftCnt <= 0) {
                val message = "${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}경품권이 없습니다 ㅠㅠ 해금을 더 진행해주세요"
                sender.sendPlainMessage(message)
                return true
            }
            giftUserMap[userName] = giftCnt - 1
            val giftSet=Item.getRandomItem()
            val giftItem=giftSet.first
            val giftItemCnt=giftSet.second
            giveReward(player = sender, rewardName = giftItem.name, rewardMaterial = giftItem, rewardCnt = giftItemCnt)

            return true
        }


        // [진행도, progress, p, 진행, 진행률, 진행률] 입력시
        if(label in arrayOf("진행도","progress","p","진행","진행률","진행률")) {
            val progress = dogam.size
            val totalCnt = targetMap.size
            val message = "${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}도감 진행도: ${ChatColor.GOLD}$progress${ChatColor.GRAY}/${ChatColor.GOLD}$totalCnt"
            sender.sendPlainMessage(message)
            return true
        }

        return true
    }
    // 타켓에있는 아이템 해금할때
    private fun unlockTarget(userName: String,itemName: String,itemCnt: Int) : Boolean {
        // 아이템 이름으로 Material 을 가져올수 있는지 확인
        val material = Material.getMaterial(itemName) ?: return false
        val player = server.getPlayer(userName) ?: return false

        // target 에 해당되는 아이템인지 확인
        if(targetMap.containsKey(itemName)) {
            // 아이템 갯수만큼 마이너스
            val targetCnt = targetMap[itemName]!!
            // 아이템 갯수만큼 player 손에서 제거
            player.inventory.removeItem(ItemStack(material,itemCnt))
            // 아이템 갯수를 줄임, 단 0보다 작으면 0으로
            targetMap[itemName] = Math.max(targetCnt-itemCnt,0)
            // 아이템 갯수가 0이면 도감에 추가하고 true 반환
            if(targetMap[itemName] == 0) {
                addToDogam(userName,itemName)
                // target 에서 제거
                targetMap.remove(itemName)
                return true
            }
        }
        return false
    }

    // 도감에 추가
    private fun addToDogam(userName: String,itemName: String) {
        if(itemName.isEmpty()) return
        if(dogam.contains(itemName)) return

        // 성공
        dogam[itemName] = userName
        unlockEvent()
        unlockBroadMessage(userName,itemName)
    }

    // 도감을 해금 갯수가 일정갯수일때 이벤트
    private fun unlockEvent() {
        // 도감이 10의 배수일때 전체 알림
        if(dogam.size>=10 && dogam.size%10 == 0) {
            // 경품권 추가
            this.server.onlinePlayers.forEach { player ->
                val userName = player.name
                val giftCnt = (giftUserMap[userName]?.plus(1)) ?: 1
                giftUserMap[userName] = giftCnt

                val message = "${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}벌써 ${ChatColor.GOLD}${dogam.size}개 ${ChatColor.GRAY}의 아이템을 해금했습니다. ${ChatColor.GOLD}경품권:$giftCnt"
                player.sendPlainMessage(message)
            }
        }

        // 진행도에 따른 리워드
        when(dogam.size) {
            22 -> giveReward(progress = dogam.size, rewardName = "당근", rewardMaterial = Material.CARROT, rewardCnt = 5)
            36 -> giveReward(progress = dogam.size, rewardName = "쇠 곡갱이", rewardMaterial = Material.IRON_PICKAXE, rewardCnt = 1)
            77 -> giveReward(progress = dogam.size, rewardName = "황금사과",rewardMaterial = Material.GOLDEN_APPLE, rewardCnt = 7)
            100 -> giveReward(progress = dogam.size, rewardName = "다이아몬드",rewardMaterial = Material.DIAMOND,rewardCnt = 10)
            150 -> giveReward(progress = dogam.size, rewardName = "에메랄드",rewardMaterial = Material.EMERALD,rewardCnt = 100)
            200 -> giveReward(progress = dogam.size, rewardName = "네더블럭",rewardMaterial = Material.NETHER_BRICK, rewardCnt = 10)
        }

    }

    // 보상지급
    private fun giveReward(
        player: Player? = null, progress: Int = 0, rewardName: String, rewardMaterial: Material, rewardCnt: Int) {
        val korRewardName = getKorName(rewardName)
        // 경품권 사용시
        if(player != null) {
            player.sendPlainMessage("${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}경품권을 사용하여 ${ChatColor.GOLD}$korRewardName $rewardCnt${ChatColor.GRAY}를 지급합니다 ^_^")
            player.inventory.addItem(ItemStack(rewardMaterial,rewardCnt))
            return
        }
        // 진행도에 따른 리워드 (전체지급)
        this.server.onlinePlayers.forEach { player ->
            player.sendPlainMessage("${ChatColor.GREEN}[알 림] ${ChatColor.GRAY}도감 진행이 ${ChatColor.GOLD}$progress${ChatColor.GRAY}가 되어 ${ChatColor.GOLD}$korRewardName $rewardCnt${ChatColor.GRAY}를 지급합니다 ^_^")
            player.inventory.addItem(ItemStack(rewardMaterial,rewardCnt))
        }
    }

    // 플레이어에게 전체 메시지를 보냄
    private fun unlockBroadMessage(userName: String, itemName: String) {
        // 메시지
        val message = "${ChatColor.BLUE}[해 금]${ChatColor.GOLD}$userName 님이 ${ChatColor.DARK_PURPLE}${getKorName(itemName)} ${ChatColor.GRAY}을(를) 해금했습니다 >_<"

        this.server.onlinePlayers.forEach { player ->
            // 경축 알림음 발생
            player.playSound(player.location, Sound.ENTITY_EGG_THROW, 1f, 1f)
            // 메시지 전송
            player.sendPlainMessage(message)
        }
    }
    
    private fun loadDogam() {
        try {
            val dogamFile = File(dataFolder, "dogam.yml")
            if(!dogamFile.exists()) {
                // create directory
                dogamFile.createNewFile()
                return
            }

            val dogamYaml = YamlConfiguration.loadConfiguration(dogamFile)
            dogamYaml.getKeys(false).forEach { key ->
                val value = dogamYaml.getString(key) ?: return@forEach
                dogam[key] = value
            }
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}도감을 불러오는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }
    
    private fun loadGift() {
        try {
            val giftFile = File(dataFolder, "gift.yml")
            if(!giftFile.exists()) {
                giftFile.createNewFile()
                return
            }

            val giftYaml = YamlConfiguration.loadConfiguration(giftFile)
            giftYaml.getKeys(false).forEach { key ->
                val value = giftYaml.getInt(key)
                giftUserMap[key] = value
            }
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}경품권을 불러오는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }

    private fun loadTarget() {
        try {
            val targetFile = File(dataFolder, "target.yml")
            if(!targetFile.exists()) {
                targetFile.createNewFile()
                return
            }

            val targetYaml = YamlConfiguration.loadConfiguration(targetFile)
            targetYaml.getKeys(false).forEach { key ->
                val value = targetYaml.getInt(key)
                targetMap[key.toMaterialName()] = value
            }
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}타겟을 불러오는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }

    // material 한글 파일 로드
    private fun loadMaterialKor() {
        try {
            val materialKorFile = File(dataFolder, "material_kor.yml")
            if(!materialKorFile.exists()) {
                materialKorFile.createNewFile()
                return
            }

            val materialKorYaml = YamlConfiguration.loadConfiguration(materialKorFile)
            materialKorYaml.getKeys(false).forEach { key ->
                val value = materialKorYaml.getString(key) ?: return@forEach
                materialKorMap[key] = value
            }
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.YELLOW}[주 의] ${ChatColor.GRAY}한글 번역 파일을 불러오는데 실패했습니다. 아이템이 영어로 표시됩니다.")
            }
        }
    }

    private fun load() {
        // data folder
        if(!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        loadDogam()
        loadGift()
        loadTarget()
        loadMaterialKor()
    }
    
    private fun saveDogam() {
        try {
            val dogamFile = File(dataFolder, "dogam.yml")
            if(!dogamFile.exists()) {
                dogamFile.createNewFile()
                return
            }

            val dogamYaml = YamlConfiguration.loadConfiguration(dogamFile)
            dogam.forEach { (key, value) ->
                dogamYaml.set(key.toMaterialName(), value)
            }
            dogamYaml.save(dogamFile)
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}도감을 저장하는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }
    
    private fun saveGift() {
        try {
            val giftFile = File(dataFolder, "gift.yml")
            if(!giftFile.exists()) {
                giftFile.createNewFile()
                return
            }

            val giftYaml = YamlConfiguration.loadConfiguration(giftFile)
            giftUserMap.forEach { key, value ->
                giftYaml.set(key, value)
            }
            giftYaml.save(giftFile)
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}경품권을 저장하는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }

    private fun saveTarget() {
        try {
            val targetFile = File(dataFolder, "target.yml")
            if(!targetFile.exists()) {
                targetFile.createNewFile()
                return
            }

            // clear file
            targetFile.writeText("")
            val targetYaml = YamlConfiguration.loadConfiguration(targetFile)
            targetMap.forEach { (key, value) ->
                targetYaml.set(key, value)
            }
            targetYaml.save(targetFile)
        } catch (e: Exception) {
            println(e.stackTrace)
            // error 메시지 안내
            this.server.onlinePlayers.forEach { player ->
                player.sendPlainMessage("${ChatColor.RED}[에 러] ${ChatColor.GRAY}타겟을 저장하는데 실패했습니다. 관리자에게 문의해주세요.")
            }
        }
    }
    
    private fun save() {
        // data folder
        if(!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        saveDogam()
        saveGift()
        saveTarget()
    }
}

// * -> ABC_DEF
private fun String.toMaterialName(): String {
    return this.replace(" ","_").toUpperCase()
}

// 아이템 이름을 한글로 변경
private fun getKorName(itemName: String): String {
    return materialKorMap[itemName] ?: itemName
}
fun main() {}