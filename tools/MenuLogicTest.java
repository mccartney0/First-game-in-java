package com.traduvertgames;

import java.lang.reflect.Field;

/**
 * Teste lógico do fluxo do Menu.java:
 * valida que Enter na tela "Como jogar" (HOW_TO_PLAY) retorna ao menu
 * principal (fix: a tela travava o jogador porque só resetava a seleção).
 *
 * Rodar: java -cp /tmp/testbin:bin com.traduvertgames.MenuLogicTest
 */
public class MenuLogicTest {
	static int pass = 0, fail = 0;

	static void check(String name, boolean ok) {
		if (ok) { pass++; System.out.println("[PASS] " + name); }
		else { fail++; System.out.println("[FAIL] " + name); }
	}

	public static void main(String[] args) throws Exception {
		Class<?> menuClass = Class.forName("com.traduvertgames.main.Menu");
		Object menu = menuClass.getConstructor().newInstance();

		Field screenField = null, optionField = null;
		for (Field f : menuClass.getDeclaredFields()) {
			if (f.getName().equals("currentScreen")) screenField = f;
			if (f.getName().equals("currentOption")) optionField = f;
		}
		screenField.setAccessible(true);
		optionField.setAccessible(true);

		Field upF = menuClass.getField("up"), downF = menuClass.getField("down"), enterF = menuClass.getField("enter");
		java.lang.reflect.Method updateMethod = menuClass.getMethod("update");

		Object howToScreen = null, mainScreen = null;
		Class<?> screenType = screenField.getType();
		Object[] enums = screenType.getEnumConstants();
		for (Object e : enums) {
			if (e.toString().equals("HOW_TO_PLAY")) howToScreen = e;
			if (e.toString().equals("MAIN")) mainScreen = e;
		}

		// Flusso realista: MAIN opção 3 ("como jogar") -> Enter -> abre tutorial
		for (int i = 0; i < 3; i++) { downF.set(menu, true); updateMethod.invoke(menu); }
		enterF.set(menu, true);
		updateMethod.invoke(menu);
		check("menu entrou na tela do tutorial (HOW_TO_PLAY)", howToScreen.equals(screenField.get(menu)));

		// O bug antigo: Enter em HOW_TO_PLAY apenas resetava currentOption=0
		// sem mudar a tela, travando o jogador.
		enterF.set(menu, true);
		updateMethod.invoke(menu);
		check("Enter no tutorial volta ao menu principal (MAIN)", mainScreen.equals(screenField.get(menu)));
		check("após voltar, seleção em cima (currentOption=0)", (int) optionField.get(menu) == 0);

		// Verificar que o EXIT_CONFIRM com "Não" também volta ao MAIN
		// (caminho adicional: MAIN opção 5 "sair" -> Não -> MAIN)
		// resetar
		downF.set(menu, true); updateMethod.invoke(menu); // opção 1
		downF.set(menu, true); updateMethod.invoke(menu); // opção 2
		downF.set(menu, true); updateMethod.invoke(menu); // opção 3
		downF.set(menu, true); updateMethod.invoke(menu); // opção 4
		downF.set(menu, true); updateMethod.invoke(menu); // opção 5 = sair
		enterF.set(menu, true);
		updateMethod.invoke(menu);
		check("menu entrou na confirmação de saída", screenField.getType().getMethod("ordinal").invoke(screenField.get(menu)).equals(5));
		enterF.set(menu, true); // "Não" (seleção inicial 0)
		updateMethod.invoke(menu);
		check("Não na confirmação volta ao menu principal", mainScreen.equals(screenField.get(menu)));

		System.out.println("== Resumo: " + pass + " passaram, " + fail + " falharam ==");
		System.exit(fail == 0 ? 0 : 1);
	}
}
