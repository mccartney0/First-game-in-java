package com.traduvertgames.entities;

import java.awt.Color;

import com.traduvertgames.main.Game;

public class EngineerNPC extends QuestNPC {

        private final double weaponRecharge;
        private final double manaBoost;

        public EngineerNPC(int x, int y) {
                this(x, y, new Color(255, 183, 77), 150, 150);
        }

        public EngineerNPC(int x, int y, Color robeColor, double weaponRecharge, double manaBoost) {
                super(x, y, robeColor);
                this.weaponRecharge = Math.max(0, weaponRecharge);
                this.manaBoost = Math.max(0, manaBoost);
        }

        @Override
        protected void onRescued() {
                if (Game.player != null) {
                        Game.player.addWeaponEnergy(weaponRecharge);
                        Game.player.addMana(manaBoost);
                }
        }
}
