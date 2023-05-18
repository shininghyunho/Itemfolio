import org.bukkit.ChatColor
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class Main : JavaPlugin() {
    override fun onEnable() {
        logger.info("Hello, world!")

    }

    override fun onDisable() {
        logger.info("Goodbye, world!")
    }

    // 모든 아이템
    private val items = mutableSetOf<ItemStack>()

    // 아이템 획득시 획득한 아이템인지 체크
    @EventHandler
    fun onPlayerPickupItemEvent(event: EntityPickupItemEvent) {
        // if is not player then return
        if (!event.entityType.isAlive) return

        // 획득한 아이템
        val item = event.item.itemStack
        // 이미 획득했다면 패스
        if (items.contains(item)) return

        // 획득한 아이템이 아니라면 추가
        items.add(item)
        // 해당 플레이어가 아이템을 획득했다고 알림
        broadcastMessage(userName = event.entity.name, itemName = item.type.name)
    }

    // 아이템 제조시 획득한 아이템인지 체크
    @EventHandler
    fun onPlayerCraftItemEvent(event: CraftItemEvent) {
        // if is not player then return
        if (!event.whoClicked.type.isAlive) return

        // 획득한 아이템
        val item = event.currentItem
        // 이미 획득했다면 패스
        if ((item == null) || items.contains(item)) return

        // 획득한 아이템이 아니라면 추가
        items.add(item)
        // 해당 플레이어가 아이템을 획득했다고 알림
        broadcastMessage(userName = event.whoClicked.name, itemName = item.type.name)
    }

    // 플레이어에게 전체 메시지를 보냄
    private fun broadcastMessage(userName: String, itemName: String) {
        // 메시지
        val message = "${ChatColor.RED}경축 ${ChatColor.GOLD}$userName 님이 ${ChatColor.GOLD}$itemName 을(를) 획득하셨습니다."

        this.server.onlinePlayers.forEach { player ->
            run {
                // 경축 알림음 발생
                player.playSound(player.location, Sound.ITEM_GOAT_HORN_SOUND_7, 1f, 1f)
                // 메시지 전송
                player.sendPlainMessage(message)
            }
        }
    }
}

fun main(args: Array<String>) {
    println("Hello World!")
}