execute if score @s sb_phase matches 0 run data merge entity @s {CustomName:'{"text":"SMPBot | wood","color":"gold"}'}
execute if score @s sb_phase matches 1 if score @s sb_tool matches 1 run data merge entity @s {CustomName:'{"text":"SMPBot | wooden pick","color":"yellow"}'}
execute if score @s sb_phase matches 1 if score @s sb_tool matches 2 run data merge entity @s {CustomName:'{"text":"SMPBot | stone pick","color":"gray"}'}
