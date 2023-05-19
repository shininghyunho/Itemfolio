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
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import kotlin.math.ceil

// 도감
private val dogam = mutableMapOf<String,String>()
// 전체 재료 갯수
private val totalMaterialCnt = org.bukkit.Material.values().size
// 전체 몬스터 갯수
private val totalMonsterCnt = org.bukkit.entity.EntityType.values().size
// 경품권 유저 map
private val giftUserMap = mutableMapOf<String,Int>()

class Main : JavaPlugin(), Listener, CommandExecutor {
    // 플러그인 활성화시
    override fun onEnable() {
        logger.info("Itemfolio is enabled!")
        // 이벤트 등록
        server.pluginManager.registerEvents(this, this)
        // 커맨드 등록
        getCommand("itemfolio")?.setExecutor(this) ?: logger.warning("itemfolio command is not registered!")
        
        // 도감, 경품권 유저 로드
        load()
    }

    // 플러그인 비활성화시
    override fun onDisable() {
        logger.info("Itemfolio is disabled!")
        // 도감, 경품권 유저 저장
        save()
    }

    // 유저가 나가면 저장
    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        save()
    }

    // 아이템 획득시 획득한 아이템인지 체크
    @EventHandler
    fun onPlayerPickupItemEvent(event: EntityPickupItemEvent) {
        // if is not player then return
        if (event.entity !is Player) return
        
        val itemName = event.item.name
        val userName = event.entity.name
        addToDogam(userName,itemName)
    }

    // 아이템 제조시 획득한 아이템인지 체크
    @EventHandler
    fun onPlayerCraftItemEvent(event: CraftItemEvent) {
        // if is not player then return
        if (event.whoClicked !is Player) return

        val itemName = event.currentItem?.run {itemMeta.displayName} ?: return
        val userName = event.whoClicked.name
        addToDogam(userName,itemName)
    }

    // 아이템을 들었을때 체크
    @EventHandler
    fun onPickupItemEvent(event: EntityPickupItemEvent) {
        // if is not player then return
        if(event.entity !is Player) return

        val itemName = event.item.name
        val userName = event.entity.name
        addToDogam(userName,itemName)
    }

    // 몬스터 처치시 처치한 몬스터인지 체크
    @EventHandler
    fun onPlayerKillEvent(event: EntityDeathEvent) {
        // killer
        val killer = event.entity.killer ?: return
        // patient
        val patient = event.entity

        // if is not player then return
        if(killer !is Player) return

        val itemName = patient.name
        val userName = killer.name
        addToDogam(userName,itemName)
    }

    // 커맨드 실행시
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        // not instanceof Player then return
        if (sender !is Player) return false

        // 명령어 안내
        if(label == "help" || label == "?"){
            sender.sendPlainMessage("${ChatColor.GRAY} 명령어 안내입니다.")
            // 진행도
            sender.sendPlainMessage("${ChatColor.GREEN} /진행도,/진행,/진행률,/progress,/p ${ChatColor.GRAY}: 도감 진행률을 확인합니다.")
            // 도감
            sender.sendPlainMessage("${ChatColor.GREEN} /도감 <숫자>,/dogam <숫자>,/d <숫자> ${ChatColor.GRAY}: 도감을 확인합니다.")
            // 순위
            sender.sendPlainMessage("${ChatColor.GREEN} /순위,/rank ${ChatColor.GRAY}: 도감 순위를 확인합니다.")
            // 경품권
            sender.sendPlainMessage("${ChatColor.GREEN} /경품권,/gift,/g ${ChatColor.GRAY}: 경품권을 확인합니다.")
            // 경품권 사용
            sender.sendPlainMessage("${ChatColor.GREEN} /경품권사용,/usegift,/ug ${ChatColor.GRAY}: 경품권을 사용합니다.")
        }
        // 도감 입력시
        if(label == "도감" || label == "dogam" || label == "d") {
            // 도감만 입력시 안내문구
            if(args == null || args.isEmpty() || args[0].toIntOrNull() == null) {
                sender.sendPlainMessage("${ChatColor.GRAY} /도감 <숫자> : 도감의 몇번째 페이지를 보실지 입력해주세요.")
                return true
            }

            // 페이지 번호
            val pageCnt = 20 // 한페이지에 보여줄 갯수
            val page = args[0].toInt()
            val totalPage = ceil(dogam.size.toDouble() / pageCnt).toInt()
            // 페이지 범위 체크
            if(page < 1 || page > totalPage) {
                sender.sendPlainMessage("${ChatColor.GRAY} /도감 <숫자> : 도감의 범위를 벗어났습니다. 1~${totalPage}까지 입력해주세요.")
                return true
            }

            // 도감이 map 형태로 되어있으므로 아이템 이름 순서를 정렬해줌
            val dogamList = dogam.toList().sortedBy { it.first }
            // 페이지에 맞는 도감을 가져옴
            val dogamPage = dogamList.subList((page-1)*pageCnt, Math.min(page*pageCnt,dogamList.size))
            // 한줄에 5개씩 보여줌
            val dogamLine = 5
            for(i in dogamPage.indices step dogamLine) {
                val dogamLineList = dogamPage.subList(i,Math.min(i+dogamLine,dogamPage.size))
                val dogamLineStr = dogamLineList.joinToString(", ") { it.first }
                sender.sendPlainMessage("${ChatColor.GRAY} $dogamLineStr")
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
                val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}${i+1}위: ${ChatColor.GOLD}$userName${ChatColor.GRAY}(${ChatColor.GOLD}$dogamCnt${ChatColor.GRAY}개)"
                sender.sendPlainMessage(message)
            }
            return true
        }

        // 경품권 입력시
        if(label == "경품권" || label == "gift" || label == "g") {
            val userName = sender.name
            val giftCnt = giftUserMap[userName] ?: 0
            val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}경품권 갯수: ${ChatColor.GOLD}$giftCnt${ChatColor.GRAY}개\n"+
                    "${ChatColor.GREEN}[알 림]${ChatColor.AQUA}/경품권사용 ${ChatColor.GRAY}입력시 경품권을 사용할 수 있습니다~!"
            sender.sendPlainMessage(message)
            return true
        }

        // 경품권사용 입력시
        if(label == "경품권사용" || label == "usegift" || label == "ug") {
            val userName = sender.name
            val giftCnt = giftUserMap[userName] ?: 0
            if(giftCnt <= 0) {
                val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}경품권이 없습니다 ㅠㅠ 해금을 더 진행해주세요"
                sender.sendPlainMessage(message)
                return true
            }
            giftUserMap[userName] = giftCnt - 1
            val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}경품권을 사용하였습니다!"
            sender.sendPlainMessage(message)
            val giftSet=Item.getRandomItem()
            val giftItem=giftSet.first
            val giftItemCnt=giftSet.second
            giveReward(player = sender, rewardName = giftItem.name, rewardMaterial = giftItem, rewardCnt = giftItemCnt)

            return true
        }


        // [진행도, progress, p, 진행, 진행률, 진행률] 입력시
        if(label in arrayOf("진행도","progress","p","진행","진행률","진행률")) {
            val progress = dogam.size
            val totalCnt = totalMaterialCnt + totalMonsterCnt
            val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}도감 진행도: ${ChatColor.GOLD}$progress${ChatColor.GRAY}/${ChatColor.GOLD}$totalCnt"
            sender.sendPlainMessage(message)
            return true
        }

        return true
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

                val message = "${ChatColor.GREEN}[알 림]${ChatColor.GRAY}벌써 ${ChatColor.GOLD}${dogam.size}개 ${ChatColor.GRAY}의 아이템을 해금했습니다. ${ChatColor.GOLD}경품권:$giftCnt"
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
        // 경품권 사용시
        if(player != null) {
            player.sendPlainMessage("${ChatColor.GREEN}[알 림]${ChatColor.GRAY}경품권을 사용하여 ${ChatColor.GOLD}$rewardName $rewardCnt${ChatColor.GRAY}를 지급합니다 ^_^")
            player.inventory.addItem(ItemStack(rewardMaterial,rewardCnt))
            return
        }
        // 진행도에 따른 리워드 (전체지급)
        this.server.onlinePlayers.forEach { player ->
            player.sendPlainMessage("${ChatColor.GREEN}[알 림]${ChatColor.GRAY}도감 진행이 ${ChatColor.GOLD}$progress${ChatColor.GRAY}가 되어 ${ChatColor.GOLD}$rewardName $rewardCnt${ChatColor.GRAY}를 지급합니다 ^_^")
            player.inventory.addItem(ItemStack(rewardMaterial,rewardCnt))
        }
    }

    // 플레이어에게 전체 메시지를 보냄
    private fun unlockBroadMessage(userName: String, itemName: String) {
        // 메시지
        val message = "${ChatColor.BLUE}[해 금]${ChatColor.GOLD}$userName 님이 ${ChatColor.DARK_PURPLE}$itemName ${ChatColor.GRAY}을(를) 해금했습니다 >_<"

        this.server.onlinePlayers.forEach { player ->
            // 경축 알림음 발생
            player.playSound(player.location, Sound.ENTITY_EGG_THROW, 1f, 1f)
            // 메시지 전송
            player.sendPlainMessage(message)
        }
    }
    
    private fun loadDogam() {
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
    }
    
    private fun loadGift() {
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
    }
    
    private fun load() {
        // data folder
        if(!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        loadDogam()
        loadGift()
    }
    
    private fun saveDogam() {
        val dogamFile = File(dataFolder, "dogam.yml")
        if(!dogamFile.exists()) {
            dogamFile.createNewFile()
            return
        }

        val dogamYaml = YamlConfiguration.loadConfiguration(dogamFile)
        dogam.forEach { key, value ->
            dogamYaml.set(key, value)
        }
        dogamYaml.save(dogamFile)
    }
    
    private fun saveGift() {
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
    }
    
    private fun save() {
        // data folder
        if(!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        saveDogam()
        saveGift()
    }
}

fun main(args: Array<String>) {
    println("Hello World!")
}