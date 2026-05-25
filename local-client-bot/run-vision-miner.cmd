@echo off
powershell.exe -ExecutionPolicy Bypass -NoProfile -File "%~dp0vision-agent.ps1" -Provider Anthropic -Goal MineStoneIron -Steps 120
