package com.eotsplus.ai.skill;

import com.eotsplus.ai.EotsSkillGoal;

public abstract class EotsSkill {
    protected final EotsSkillGoal g;
    protected int t;
    protected int state;
    protected int st;
    private boolean finished;

    protected EotsSkill(EotsSkillGoal g) {
        this.g = g;
    }

    public abstract void start();

    protected abstract void tick();

    public void stop() {
    }

    public final void tickSkill() {
        this.tick();
        this.t++;
        this.st++;
    }

    protected void setState(int state) {
        this.state = state;
        this.st = -1;
    }

    protected void finish() {
        this.finished = true;
    }

    public boolean isFinished() {
        return this.finished;
    }
}
