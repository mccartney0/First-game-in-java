import com.traduvertgames.main.OptionsConfig;

public class VolumeProbe {
	public static void main(String[] args) {
		float v0 = OptionsConfig.getMusicVolume();
		System.out.println("antes: " + v0);
		OptionsConfig.adjustMusicVolume(2);
		float v1 = OptionsConfig.getMusicVolume();
		System.out.println("depois +2: " + v1);
		System.out.println("esperado: " + (v0 + 2.0f));
		System.out.println("igual: " + (v1 == v0 + 2.0f));
	}
}
