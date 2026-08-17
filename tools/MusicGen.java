import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * Gerador procedural de trilhas de fundo do jogo (rodada 22). Grava WAV PCM
 * 44.1 kHz / 16-bit / mono com loop suave (fade-out no final) em res/sounds/.
 *
 * Temas:
 *  - music_forest.wav  — floresta calma (fases 1-2): pad quente + pluck pentatônico
 *  - music_tension.wav — base inimiga (fases 3-5): pulso grave + arpejo menor
 *  - music_boss.wav    — núcleo/arena do chefe (fases 6-8): stabs menores 130 BPM
 *  - music_arena.wav   — modo infinito: adrenalina 140 BPM
 *
 * Uso: javac tools/MusicGen.java && java -cp . MusicGen (executar na raiz do projeto)
 */
public class MusicGen {

	private static final int SAMPLE_RATE = 44100;
	private static final double DURATION = 72.0; // segundos
	private static final double FADE_OUT = 5.0;  // fade final para loop suave

	public static void main(String[] args) throws IOException {
		File outDir = new File("res/sounds");
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		Random rng = new Random(42L);

		writeWave(new File(outDir, "music_forest.wav"), forest(rng));
		writeWave(new File(outDir, "music_tension.wav"), tension(rng));
		writeWave(new File(outDir, "music_boss.wav"), boss(rng));
		writeWave(new File(outDir, "music_arena.wav"), arena(rng));

		System.out.println("Trilhas geradas em " + outDir.getAbsolutePath());
	}

	/* ------------------------------------------------------------------ */
	/* Tema 1 — floresta calma: pad quente + pluck pentatônico maior, 72 BPM */
	/* ------------------------------------------------------------------ */
	private static double[] forest(Random rng) {
		int n = (int) (SAMPLE_RATE * DURATION);
		double[] out = new double[n];
		double bpm = 72.0;
		double beat = 60.0 / bpm;
		// Escala pentatônica maior em D maior: D E F# A B (oitavas 3-4)
		double[] scale = {146.83, 164.81, 185.00, 220.00, 246.94};
		// Progressão I - V - vi - IV (D A Bm G) em notas de baixo
		double[] bass = {73.42, 55.00, 61.74, 49.00};

		for (double t = 0.0; t < DURATION; t += 1.0 / SAMPLE_RATE) {
			int i = (int) (t * SAMPLE_RATE);
			if (i >= n) break;
			double bar = t / (beat * 4);
			int barIdx = (int) bar % 4;
			double barPos = bar % 1;

			// Pad: acorde de terças sobre a progressão, envelope lento
			double chordRoot = bass[barIdx];
			double pad = 0;
			for (double off : new double[]{0, 4, 7, 11}) {
				double freq = chordRoot * Math.pow(2, off / 12.0) * 2; // oitava acima
				pad += Math.sin(2 * Math.PI * freq * t) * 0.045;
			}
			// Tremolo suave do pad (respiração)
			pad *= 0.5 + 0.5 * Math.sin(2 * Math.PI * 0.17 * t);

			// Pluck: notas da escala a cada 1/2 batida, com chance
			double beatPos = t / beat;
			if (Math.abs(beatPos - Math.round(beatPos)) < 0.012 || Math.abs(beatPos % 1 - 0.5) < 0.012) {
				double freq = scale[(int) (Math.abs(Math.sin(t * 0.37 + rng.nextDouble() * 6)) * scale.length) % scale.length];
				double decay = Math.exp(-((beatPos % 1)) * 4.5) * 0.16;
				pad += Math.sin(2 * Math.PI * freq * t) * decay;
				// oitava aguda pontual
				pad += Math.sin(2 * Math.PI * freq * 2 * t) * decay * 0.25;
			}

			// Baixo suave a cada 1 batida
			double bassPulse = Math.exp(-(beatPos % 1) * 3.0) * 0.10;
			pad += Math.sin(2 * Math.PI * chordRoot * t) * bassPulse;

			// Ruído de "vento" leve
			double noise = (rng.nextDouble() - 0.5) * 0.006 * (0.7 + 0.3 * Math.sin(2 * Math.PI * 0.09 * t));
			out[i] = pad + noise;
		}
		return envelope(out, n);
	}

	/* --------------------------------------------------------------- */
	/* Tema 2 — tensão: pulso grave 80 BPM + arpejo menor, filtro tenso */
	/* --------------------------------------------------------------- */
	private static double[] tension(Random rng) {
		int n = (int) (SAMPLE_RATE * DURATION);
		double[] out = new double[n];
		double bpm = 80.0;
		double beat = 60.0 / bpm;
		// Tônica: A menor (A2 = 110 Hz). Notas do arpejo: A C E G (menor 7)
		double[] arp = {110.00, 130.81, 164.81, 196.00, 164.81, 130.81};

		for (double t = 0.0; t < DURATION; t += 1.0 / SAMPLE_RATE) {
			int i = (int) (t * SAMPLE_RATE);
			if (i >= n) break;
			double beatPos = t / beat;
			double bar = t / (beat * 8);
			int halfBar = (int) (bar * 2) % 2;

			// Pulso grave (sidechain-like): 2 batidas de "kick" sintético por compasso
			double kick = 0;
			double inBeat = beatPos % 1;
			if (inBeat < 0.09) {
				double env = Math.exp(-inBeat * 38);
				double freq = 60 + 25 * Math.exp(-inBeat * 25);
				kick = Math.sin(2 * Math.PI * freq * t) * env * 0.28;
			}
			double sub = 0;
			if (inBeat < 0.45 && (beatPos % 2) < 1) {
				sub = Math.sin(2 * Math.PI * 55 * t) * 0.09 * Math.exp(-inBeat * 2);
			}

			// Arpejo em semicolcheias (2 por batida)
			double arpNote = arp[(int) (beatPos * 2) % arp.length];
			// Alternância de padrão a cada 2 compassos para variedade
			if (halfBar == 1) {
				arpNote *= 1.5; // quinta acima no segundo meio
			}
			double arpEnv = Math.exp(-((beatPos * 2) % 1) * 6) * 0.13;
			double saw = sawtooth(2 * Math.PI * arpNote * t) * 0.6;
			double arpTone = (0.7 * Math.sin(2 * Math.PI * arpNote * t) + 0.3 * saw) * arpEnv;
			// "filtro" tenso: atenua um pouco os agudos do arpejo
			double arpFreq = Math.max(0.3, 0.55 + 0.3 * Math.sin(2 * Math.PI * 0.04 * t));
			arpTone *= arpFreq;

			// Drone tenso (trítono sutil)
			double drone = Math.sin(2 * Math.PI * 116.54 * t) * 0.035
					+ Math.sin(2 * Math.PI * 155.56 * t) * 0.025;

			out[i] = kick + sub + arpTone + drone;
		}
		return envelope(out, n);
	}

	/* ------------------------------------------------------------------ */
	/* Tema 3 — chefe: 130 BPM, stabs menores, baixo pulsante, adrenalina */
	/* ------------------------------------------------------------------ */
	private static double[] boss(Random rng) {
		int n = (int) (SAMPLE_RATE * DURATION);
		double[] out = new double[n];
		double bpm = 130.0;
		double beat = 60.0 / bpm;
		// Tônica: E menor. Stab: acorde E5 (E G B) em oitava 4-5
		double[] stab = {164.81 * 2, 196.00 * 2, 246.94 * 2}; // E4 G4 B4 -> x2 => E5
		double bassRoot = 41.20; // E1

		for (double t = 0.0; t < DURATION; t += 1.0 / SAMPLE_RATE) {
			int i = (int) (t * SAMPLE_RATE);
			if (i >= n) break;
			double beatPos = t / beat;
			double inBeat = beatPos % 1;
			int eighth = (int) (beatPos * 2) % 8;

			// Bateria: kick nos tempos 1 e 3, snare sintético nos 2 e 4
			double drums = 0;
			if (inBeat < 0.07) {
				double env = Math.exp(-inBeat * 45);
				drums += Math.sin(2 * Math.PI * (62 + 30 * Math.exp(-inBeat * 30)) * t) * env * 0.30;
			}
			double snarePos = inBeat - 0.5;
			if (snarePos >= 0 && snarePos < 0.10) {
				double env = Math.exp(-snarePos * 40);
				drums += (Math.sin(2 * Math.PI * 210 * t) * 0.4 + (rng.nextDouble() - 0.5) * 0.8) * env * 0.14;
			}
			// Hi-hat nas semicolcheias
			if (inBeat < 0.03) {
				drums += (rng.nextDouble() - 0.5) * 0.035 * (eighth % 2 == 1 ? 1.6 : 0.9);
			}

			// Baixo pulsante em colcheias
			double bassEnv = Math.exp(-(inBeat % 0.5) * 5) * 0.12;
			double bassFreq = (eighth % 4 < 2) ? bassRoot : bassRoot * 1.5; // E1 / B1
			drums += Math.sin(2 * Math.PI * bassFreq * t) * bassEnv;

			// Stab menor nos compassos ímpares (pattern call-and-response)
			double stabAmp = 0;
			int barEighth = (int) (beatPos * 2) % 8;
			if (barEighth == 0 || barEighth == 3 || barEighth == 5) {
				double env = Math.exp(-((beatPos * 2) % 1) * 5) * 0.12;
				for (double s : stab) {
					stabAmp += (0.6 * Math.sin(2 * Math.PI * s * t) + 0.4 * sawtooth(2 * Math.PI * s * t)) * env;
				}
			}
			// Lead agudo pontual (motivo de tensão) a cada 4 compassos
			double bar = t / (beat * 4);
			if (((int) (bar * 8) % 8) == 0 && inBeat < 0.35) {
				double lead = 0;
				double[] motif = {659.25, 587.33, 523.25, 493.88}; // E5 D5 C5 B4
				double f = motif[(int) (inBeat * 12) % motif.length];
				lead = Math.sin(2 * Math.PI * f * t) * Math.exp(-inBeat * 4) * 0.10;
				stabAmp += lead;
			}

			out[i] = drums + stabAmp;
		}
		return envelope(out, n);
	}

	/* --------------------------------------------------------------- */
	/* Tema 4 — arena (modo infinito): 140 BPM, adrenalina, energia     */
	/* --------------------------------------------------------------- */
	private static double[] arena(Random rng) {
		int n = (int) (SAMPLE_RATE * DURATION);
		double[] out = new double[n];
		double bpm = 140.0;
		double beat = 60.0 / bpm;
		// Tônica: G menor. Arpejo rápido: G Bb D F
		double[] arp = {98.00 * 2, 116.54 * 2, 146.83 * 2, 174.61 * 2}; // G4 Bb4 D5 F5
		double bassG = 49.00; // G1

		for (double t = 0.0; t < DURATION; t += 1.0 / SAMPLE_RATE) {
			int i = (int) (t * SAMPLE_RATE);
			if (i >= n) break;
			double beatPos = t / beat;
			double inBeat = beatPos % 1;
			int sixteenth = (int) (beatPos * 4) % 16;

			// Bateria rápida: four-on-the-floor com ghost notes
			double drums = 0;
			if (inBeat < 0.06) {
				double env = Math.exp(-inBeat * 50);
				drums += Math.sin(2 * Math.PI * (66 + 28 * Math.exp(-inBeat * 32)) * t) * env * 0.28;
			}
			double snPos = inBeat - 0.5;
			if (snPos >= 0 && snPos < 0.08) {
				double env = Math.exp(-snPos * 42);
				drums += (Math.sin(2 * Math.PI * 205 * t) * 0.4 + (rng.nextDouble() - 0.5) * 0.9) * env * 0.13;
			}
			if (inBeat < 0.025) {
				drums += (rng.nextDouble() - 0.5) * 0.04 * (sixteenth % 2 == 1 ? 1.5 : 0.8);
			}

			// Baixo em colcheias (drive)
			double bassEnv = Math.exp(-(inBeat % 0.5) * 6) * 0.13;
			double bassFreq = (sixteenth % 8 < 4) ? bassG : bassG * 1.5;
			drums += Math.sin(2 * Math.PI * bassFreq * t) * bassEnv;

			// Arpejo em semicolcheias com padrão ascendente/descendente
			double arpPos = (beatPos * 4) % 8;
			int arpIdx = (int) arpPos;
			if (arpPos >= 4) {
				arpIdx = 7 - arpIdx;
			}
			arpIdx = Math.max(0, Math.min(3, arpIdx));
			double arpEnv = Math.exp(-((beatPos * 4) % 1) * 7) * 0.11;
			double freq = arp[arpIdx];
			double saw = sawtooth(2 * Math.PI * freq * t) * 0.55;
			drums += (0.65 * Math.sin(2 * Math.PI * freq * t) + 0.35 * saw) * arpEnv;

			// Pad de fundo (sustentação harmônica)
			double pad = Math.sin(2 * Math.PI * 196.00 * t) * 0.03
					+ Math.sin(2 * Math.PI * 233.08 * t) * 0.02
					+ Math.sin(2 * Math.PI * 293.66 * t) * 0.015;
			drums += pad;

			out[i] = drums;
		}
		return envelope(out, n);
	}

	/* --------------------------- utilitários -------------------------- */

	/** Onda dente-de-serra (0..1), usada para timbre sintético. */
	private static double sawtooth(double phase) {
		return 2.0 * ((phase / (2 * Math.PI)) - Math.floor(0.5 + phase / (2 * Math.PI)));
	}

	/** Fade-out final para permitir loop suave do Clip. */
	private static double[] envelope(double[] samples, int n) {
		int fadeStart = (int) ((DURATION - FADE_OUT) * SAMPLE_RATE);
		for (int i = 0; i < n; i++) {
			if (i >= fadeStart) {
				double k = 1.0 - ((double) (i - fadeStart) / (n - fadeStart));
				samples[i] *= k * k; // curva quadrática (mais suave no início do fade)
			}
			// Limite suave para evitar distorção
			double v = samples[i];
			if (v > 0.98) v = 0.98 + 0.02 * Math.tanh((v - 0.98) * 10);
			else if (v < -0.98) v = -(0.98 + 0.02 * Math.tanh((-v - 0.98) * 10));
			samples[i] = v;
		}
		return samples;
	}

	/** Grava array de amostras como WAV 16-bit mono. */
	private static void writeWave(File file, double[] samples) throws IOException {
		int bytesPerSample = 2;
		int channels = 1;
		int blockAlign = channels * bytesPerSample;
		int byteRate = SAMPLE_RATE * blockAlign;
		int dataSize = samples.length * bytesPerSample;

		try (FileOutputStream fos = new FileOutputStream(file)) {
			writeHeader(fos, dataSize, SAMPLE_RATE, blockAlign, byteRate);
			for (double s : samples) {
				int pcm = (int) Math.max(-32768, Math.min(32767, Math.round(s * 32767)));
				fos.write(pcm & 0xFF);
				fos.write((pcm >> 8) & 0xFF);
			}
		}
		System.out.println("  " + file.getName() + " (" + (dataSize / 1024) + " KB, "
				+ String.format("%.1f", samples.length / (double) SAMPLE_RATE) + " s)");
	}

	private static void writeHeader(FileOutputStream fos, int dataSize, int sampleRate,
			int blockAlign, int byteRate) throws IOException {
		int total = 36 + dataSize;
		write(fos, "RIFF");
		writeInt(fos, total);
		write(fos, "WAVE");
		write(fos, "fmt ");
		writeInt(fos, 16);                  // chunk size
		writeShort(fos, 1);                 // PCM
		writeShort(fos, 1);                 // mono
		writeInt(fos, sampleRate);
		writeInt(fos, byteRate);
		writeShort(fos, blockAlign);
		writeShort(fos, 16);                // bits por amostra
		write(fos, "data");
		writeInt(fos, dataSize);
	}

	private static void write(FileOutputStream fos, String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			fos.write(s.charAt(i));
		}
	}

	private static void writeInt(FileOutputStream fos, int v) throws IOException {
		fos.write(v & 0xFF);
		fos.write((v >> 8) & 0xFF);
		fos.write((v >> 16) & 0xFF);
		fos.write((v >> 24) & 0xFF);
	}

	private static void writeShort(FileOutputStream fos, int v) throws IOException {
		fos.write(v & 0xFF);
		fos.write((v >> 8) & 0xFF);
	}
}
