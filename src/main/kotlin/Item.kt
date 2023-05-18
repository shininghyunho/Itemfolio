import org.bukkit.Material

private val EATABLE_GIFT_RATE=0.9 // 경품권 사용시 먹을거 나올확률
private val RARE_EATABLE_RATE=0.1 // 레어 먹을거 나올확률

// 먹을거
private val commonEatable = listOf(
    Material.APPLE,
    Material.BAKED_POTATO,
    Material.BREAD,
    Material.CARROT,
    Material.COOKED_BEEF,
    Material.COOKED_CHICKEN,
    Material.POTATO,
)
private val rareEatable = listOf(
    Material.CHORUS_FRUIT,
    Material.BEETROOT,
    Material.BEETROOT_SOUP,
    Material.COOKED_COD,
    Material.COOKED_MUTTON,
    Material.COOKED_PORKCHOP,
    Material.COOKED_RABBIT,
    Material.COOKED_SALMON,
    Material.COOKIE,
    Material.DRIED_KELP,
    Material.ENCHANTED_GOLDEN_APPLE,
    Material.GOLDEN_APPLE,
    Material.GOLDEN_CARROT,
    Material.HONEY_BOTTLE,
    Material.MELON_SLICE,
    Material.MUSHROOM_STEW,
    Material.POISONOUS_POTATO,
    Material.PUFFERFISH,
    Material.PUMPKIN_PIE,
    Material.RABBIT_STEW,
    Material.ROTTEN_FLESH,
    Material.SUSPICIOUS_STEW,
    Material.SWEET_BERRIES,
    Material.TROPICAL_FISH
)

// 금속류
private val commonMetalMap = mapOf(
    1.0 to Material.IRON_INGOT,
    0.5 to Material.GOLD_INGOT,
    0.2 to Material.EMERALD,
    0.01 to Material.DIAMOND,
    0.005 to Material.NETHERITE_SCRAP,
)

// 확률 맵
private val probabilityMap = mapOf<Double,Int>(
    1.0 to 1,
    0.7 to 2,
    0.4 to 3,
    0.1 to 8,
    0.05 to 10,
    0.03 to 20,
    0.01 to 32,
    0.005 to 128,
    0.001 to 256,
)
class Item {
    companion object {
        // 랜덤으로 아이템과 갯수를 얻음
        private fun getRandomEatable(): Pair<Material, Int> {
            // 아이템 결정
            // 0 이상 1 이하 랜덤값
            val itemRate = Math.random()
            // 90% 확률로 commonEatable
            val randomMaterial = if(itemRate<=1-RARE_EATABLE_RATE) {
                commonEatable.random()
            } else {
                rareEatable.random()
            }

            // 갯수 결정
            val propRate = Math.random()
            val randomCnt = probabilityMap.filterKeys { it >= propRate }.maxOf { it.value }
            return Pair(randomMaterial, randomCnt)
        }

        private fun getRandomMetal(): Pair<Material, Int> {
            // 아이템 결정
            val itemRate = Math.random()
            val randomMaterial = commonMetalMap.filterKeys { it >= itemRate }.maxOf { it.value }

            // 갯수 결정
            val propRate = Math.random()
            val randomCnt = probabilityMap.filterKeys { it >= propRate }.maxOf { it.value }
            return Pair(randomMaterial, randomCnt)
        }

        // 랜덤 아이템을 반환
        fun getRandomItem(): Pair<Material, Int> {
            return if(Math.random()<=EATABLE_GIFT_RATE) getRandomEatable() else getRandomMetal()
        }
    }
}