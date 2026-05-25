scoreboard players set #mined sb_tmp 0
function smpbots:bot/mine_nearby
execute if score #mined sb_tmp matches 0 run function smpbots:bot/move
