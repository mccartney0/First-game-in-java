package com.traduvertgames.graficos;

/** Regressão do cálculo de distância exibido no waypoint. */
public final class MissionHudDistanceTest {
    private MissionHudDistanceTest() {
    }

    public static void main(String[] args) {
        check("64 pixels de mundo = 4m", "4m".equals(MissionHud.formatDistanceMeters(64)));
        check("240 pixels de mundo = 15m", "15m".equals(MissionHud.formatDistanceMeters(240)));
        check("distância negativa não gera valor negativo", "0m".equals(MissionHud.formatDistanceMeters(-1)));
        System.out.println("MissionHudDistanceTest: aprovado");
    }

    private static void check(String description, boolean condition) {
        if (!condition) {
            throw new AssertionError(description);
        }
        System.out.println("[PASS] " + description);
    }
}
