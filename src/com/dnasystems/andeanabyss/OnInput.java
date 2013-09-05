package com.dnasystems.andeanabyss;

public interface OnInput {

    public boolean mMoved = false;

    public boolean onUp(int id, float x, float y);
    public boolean onDown(int id, float x, float y);
    public boolean onMove(int id, float dx, float dy);
    public boolean onZoom(float scalefactor);
}
