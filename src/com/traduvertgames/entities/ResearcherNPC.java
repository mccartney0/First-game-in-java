package com.traduvertgames.entities;

import java.awt.Color;

import com.traduvertgames.main.Game;

public class ResearcherNPC extends QuestNPC {

        public ResearcherNPC(int x, int y) {
                super(x, y, new Color(126, 87, 194));
        }

        @Override
        protected void onRescued() {
                Game.applyComboSurge(1, Game.getComboBaseDuration());
        }
}
