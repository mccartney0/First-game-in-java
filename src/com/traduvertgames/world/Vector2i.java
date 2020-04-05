package com.traduvertgames.world;

//Guardar posições no mundo
public class Vector2i {

	public int x,y;

	public Vector2i(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}


	//Verifica objeto
	@Override
	public boolean equals(Object obj) {
		Vector2i vec = (Vector2i) obj;
		if(vec.x == this.x && vec.y == this.y) {
			return true;
		}else {
			return false;
		}
	}
	
	
}
