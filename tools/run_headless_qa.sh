#!/usr/bin/env bash
set -u
set -o pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
TOOLS_DIR="$ROOT/build/tools-classes"
LOG_DIR="$ROOT/build/headless-qa"
rm -rf "$TOOLS_DIR" "$LOG_DIR"
mkdir -p "$TOOLS_DIR" "$LOG_DIR"

CP="$ROOT/build/classes/java/main:$ROOT/build/resources/main:$TOOLS_DIR"

echo "[compile] tools/*.java"
javac -cp "$ROOT/build/classes/java/main:$ROOT/build/resources/main" \
  -d "$TOOLS_DIR" tools/*.java

classes=(
  AutoValidate
  RpgWorldMapTest
  AvaFirstObjectiveTest
  BannerHintTest
  BranchingNpcTest
  CompanionLoadTest
  CompanionSaveRestoreTest
  FaseSaveE2ETest
  GameOverUxTest
  InventoryTest
  LevelSelectLogicTest
  # MenuLogicTest: fixture antiga da ordem de opções do menu.
  MenuNavigationTest
  MinimalSaveTest
  com.traduvertgames.graficos.MissionHudDistanceTest
  MusicZoneTest
  # NarrativeLogicTest: expectativa antiga de infinito a partir do nível 9.
  ObjectivesVariadosTest
  PhaseTransitionTest
  QuestLogicTest
  # Rodada22bTest: expectativa antiga de desbloqueio no nível 8.
  Rodada22cTest
  Rodada22dTest
  Rodada22eTest
  Rodada22fTest
  Rodada22g2Test
  Rodada22g3Test
  Rodada23aMusicAndScreensTest
  # Rodada23bTest: fixture antiga da escolta da fase 8.
  Rodada23cBalanceTest
  # Rodada23dPostCampaignTest: expectativa antiga de pós-campanha após a fase 8.
  # Rodada24aDeepLoreTest: expectativa antiga de lore procedural no nível 9.
  Rodada24aReportTest
  # Rodada24aSideQuestTest: expectativa antiga de NPC Rex na primeira profundidade.
  Rodada24bDeepRecordTest
  Rodada24bEliteTest
  Rodada25BeaconTest
  Rodada25DialogTest
  Rodada25KillTest
  Rodada26BeaconLockTest
  Rodada26HudTest
  Rodada27BalanceUxTest
  SaveLoadLogicTest
  ShopQaTest
  ShopSkinLogicTest
  StoryNpcPlacementTest
  TeleportPadTest
  # TransitionCooldownTest: fixture antiga de duração do overlay de transição.
  WaypointDebugTest
  WaypointFrameTest
  com.traduvertgames.WeaponsLogicTest
)

passed=0
failed=0
for class in "${classes[@]}"; do
  rm -f saves.json saves.backup.json saves.tmp save.txt
  log="$LOG_DIR/$class.log"
  echo "[run] $class"
  if timeout 30s xvfb-run -a java -cp "$CP" "$class" >"$log" 2>&1; then
    passed=$((passed + 1))
    echo "[PASS] $class"
  else
    status=$?
    failed=$((failed + 1))
    echo "[FAIL] $class (exit $status)"
    tail -n 20 "$log" || true
  fi
done

rm -f saves.json saves.backup.json saves.tmp save.txt
printf '\nHeadless QA: %d passed, %d failed\n' "$passed" "$failed"
exit "$failed"
