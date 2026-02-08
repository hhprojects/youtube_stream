import { Audio } from 'expo-audio';

export type PlaybackStatus = {
  isPlaying: boolean;
  isBuffering: boolean;
  positionMillis: number;
  durationMillis: number;
  didJustFinish: boolean;
};

class AudioPlayer {
  private player: Audio.AudioPlayer | null = null;
  private listeners: Array<(status: PlaybackStatus) => void> = [];
  private currentUrl: string | null = null;
  private playbackStatus: PlaybackStatus = {
    isPlaying: false,
    isBuffering: false,
    positionMillis: 0,
    durationMillis: 0,
    didJustFinish: false,
  };
  private statusUpdateSubscription: any = null;

  async load(url: string): Promise<void> {
    if (this.player) {
      await this.unload();
    }

    try {
      // Create audio source
      const { sound } = await Audio.Sound.createAsync(
        { uri: url },
        {
          shouldPlay: false,
          progressUpdateIntervalMillis: 1000,
        },
        this.onPlaybackStatusUpdate
      );

      this.player = sound;
      this.currentUrl = url;
    } catch (error) {
      console.error('Failed to load audio:', error);
      throw error;
    }
  }

  async play(): Promise<void> {
    if (!this.player) {
      throw new Error('No audio loaded');
    }

    try {
      await this.player.playAsync();
      this.playbackStatus.isPlaying = true;
      this.notifyListeners();
    } catch (error) {
      console.error('Failed to play audio:', error);
      throw error;
    }
  }

  async pause(): Promise<void> {
    if (!this.player) return;

    try {
      const status = await this.player.getStatusAsync();
      if (status.isLoaded && status.isPlaying) {
        await this.player.pauseAsync();
        this.playbackStatus.isPlaying = false;
        this.notifyListeners();
      }
    } catch (error) {
      console.error('Failed to pause audio:', error);
    }
  }

  async stop(): Promise<void> {
    if (!this.player) return;

    try {
      const status = await this.player.getStatusAsync();
      if (status.isLoaded && status.isPlaying) {
        await this.player.stopAsync();
        await this.player.setPositionAsync(0);
        this.playbackStatus.isPlaying = false;
        this.playbackStatus.positionMillis = 0;
        this.notifyListeners();
      }
    } catch (error) {
      console.error('Failed to stop audio:', error);
    }
  }

  async seek(positionMillis: number): Promise<void> {
    if (!this.player) return;

    try {
      await this.player.setPositionAsync(positionMillis);
      this.playbackStatus.positionMillis = positionMillis;
      this.notifyListeners();
    } catch (error) {
      console.error('Failed to seek audio:', error);
    }
  }

  async unload(): Promise<void> {
    if (this.player) {
      try {
        await this.player.unloadAsync();
        this.player = null;
        this.currentUrl = null;
      } catch (error) {
        console.error('Failed to unload audio:', error);
      }
    }
  }

  onPlaybackStatusUpdate = (status: any): void => {
    if (!status.isLoaded) return;

    this.playbackStatus = {
      isPlaying: status.isPlaying ?? false,
      isBuffering: status.isBuffering ?? false,
      positionMillis: status.positionMillis ?? 0,
      durationMillis: status.durationMillis ?? 0,
      didJustFinish: status.didJustFinish ?? false,
    };

    this.notifyListeners();
  };

  addListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners.push(listener);
  }

  removeListener(listener: (status: PlaybackStatus) => void): void {
    this.listeners = this.listeners.filter(l => l !== listener);
  }

  private notifyListeners() {
    this.listeners.forEach(listener => listener({ ...this.playbackStatus }));
  }

  getCurrentUrl(): string | null {
    return this.currentUrl;
  }
}

export const audioPlayer = new AudioPlayer();

// Setup audio mode
export const setupAudio = async (): Promise<void> => {
  try {
    await Audio.setAudioModeAsync({
      playsInSilentModeIOS: true,
      staysActiveInBackground: true,
      shouldDuckAndroid: true,
    });

    console.log('Audio mode configured successfully');
  } catch (error) {
    console.error('Failed to setup audio:', error);
  }
};
