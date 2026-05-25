execute if block ~ ~-1 ~ minecraft:stone run tp @s ~ ~-1 ~
execute if block ~ ~-1 ~ minecraft:deepslate run tp @s ~ ~-1 ~
execute if block ~1 ~ ~ minecraft:stone run tp @s ~1 ~ ~
execute if block ~-1 ~ ~ minecraft:stone run tp @s ~-1 ~ ~
execute if block ~ ~ ~1 minecraft:stone run tp @s ~ ~ ~1
execute if block ~ ~ ~-1 minecraft:stone run tp @s ~ ~ ~-1
execute unless block ~ ~-1 ~ minecraft:stone unless block ~ ~-1 ~ minecraft:deepslate run tp @s ~0.5 ~ ~
