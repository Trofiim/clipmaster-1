package com.clipmaster.ai;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaItem.ClippingConfiguration;
import androidx.media3.transformer.Transformer;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;

public class VideoTrimmer {

    public interface Callback {
        void onSuccess(String message);
        void onError(String message);
    }

    public static void trim(
            Context context,
            Uri uri,
            int seconds,
            Callback callback) {

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .setClippingConfiguration(
                        new ClippingConfiguration.Builder()
                                .setStartPositionMs(0)
                                .setEndPositionMs(seconds * 1000L)
                                .build())
                .build();

        EditedMediaItem editedItem =
                new EditedMediaItem.Builder(mediaItem).build();

        Transformer transformer =
                new Transformer.Builder(context)
                        .addListener(new Transformer.Listener() {
                            @Override
                            public void onCompleted(
                                    EditedMediaItem item,
                                    ExportResult result) {
                                callback.onSuccess("Видео обработано.");
                            }

                            @Override
                            public void onError(
                                    EditedMediaItem item,
                                    ExportException exception) {
                                callback.onError(exception.getMessage());
                            }
                        })
                        .build();
    }
}
            StringBuilder result = new StringBuilder();
            int durationUs = seconds * 1000000;

            MediaFormat videoFormat = extractor.getTrackFormat(videoTrack);
            long totalDuration = videoFormat.containsKey(MediaFormat.KEY_DURATION)
                    ? videoFormat.getLong(MediaFormat.KEY_DURATION) : 0;

            if (totalDuration <= 0) return "Не удалось определить длительность видео.";

            int count = (int) Math.ceil((double) totalDuration / durationUs);

            for (int part = 0; part < count; part++) {
                long start = (long) part * durationUs;
                long end = Math.min(start + durationUs, totalDuration);

                extractor.seekTo(start, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

                String path = new File(folder, "Clip_" + (part + 1) + ".mp4").getAbsolutePath();

                muxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                int[] outputTracks = new int[extractor.getTrackCount()];
                for (int i = 0; i < outputTracks.length; i++) outputTracks[i] = -1;

                extractor.selectTrack(videoTrack);
                outputTracks[videoTrack] = muxer.addTrack(videoFormat);

                if (audioTrack >= 0) {
                    MediaFormat audioFormat = extractor.getTrackFormat(audioTrack);
                    extractor.selectTrack(audioTrack);
                    outputTracks[audioTrack] = muxer.addTrack(audioFormat);
                }

                muxer.start();

                ByteBuffer buffer = ByteBuffer.allocate(2 * 1024 * 1024);
                android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
                while (true) {
                    int track = extractor.getSampleTrackIndex();
                    if (track < 0) break;

                    long time = extractor.getSampleTime();
                    if (time >= end) break;

                    int size = extractor.readSampleData(buffer, 0);
                    if (size < 0) break;

                    info.offset = 0;
                    info.size = size;
                    info.presentationTimeUs = Math.max(0, time - start);
                    info.flags = extractor.getSampleFlags();

                    if (outputTracks[track] >= 0) {
                        muxer.writeSampleData(outputTracks[track], buffer, info);
                    }

                    extractor.advance();
                }

                muxer.stop();
                muxer.release();
                muxer = null;

                extractor.unselectTrack(videoTrack);
                if (audioTrack >= 0) extractor.unselectTrack(audioTrack);

                result.append(path).append("\n");
            }

            return "Готово! Создано клипов: " + count + "\n" + result;

        } catch (Exception e) {
            return "Ошибка нарезки: " + e.getMessage();
        } finally {
            if (muxer != null) {
                try { muxer.release(); } catch (Exception ignored) {}
            }
            extractor.release();
        }
    }
}
