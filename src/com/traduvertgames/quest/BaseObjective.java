package com.traduvertgames.quest;

public abstract class BaseObjective implements RPGObjective {
    private final String title;
    private final String description;

    protected BaseObjective(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
