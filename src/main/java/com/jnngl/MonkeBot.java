package com.jnngl;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class MonkeBot extends ListenerAdapter {
    private static final FrameConverter<BufferedImage> FRAME_CONVERTER = new Java2DFrameConverter();
    private static final Font FONT;

    static {
        try {
            FONT = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream("font.ttf"));
        } catch (FontFormatException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public MonkeBot() throws IOException {
        JDABuilder.createDefault(Files.readString(Path.of("token.txt")).trim())
                .setActivity(Activity.of(Activity.ActivityType.WATCHING, "обезьянок"))
                .setStatus(OnlineStatus.IDLE)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(this)
                .build();
    }

    public static void main(String[] args) throws IOException {
        new MonkeBot();
    }

    public Optional<String> getAttachmentUrl(Message message) {
        for (Message.Attachment attachment : message.getAttachments()) {
            if (attachment.isImage()) {
                return Optional.of(attachment.getUrl());
            }
        }

        return Optional.empty();
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        if (!message.startsWith("!monke")) {
            return;
        }

        String[] args = message.split(" ", 2);
        if (args.length == 1) {
            event.getMessage().reply("Использование: !monke [ссылка] <текст>\n" +
                    "Также можно ответить на сообщение с гифкой, в таком случае команда " +
                    "выглядит так: !monke <текст>").queue();
            return;
        }

        String text = args[1];
        String url;

        if (args[1].startsWith("https://") || args[1].startsWith("http://")) {
            args = args[1].split(" ", 2);
            if (args.length == 1) {
                event.getMessage().reply("Текст не указан.").queue();
                return;
            }

            url = args[0];
            text = args[1];
        } else if (event.getMessage().getReferencedMessage() != null) {
            Message referenced = event.getMessage().getReferencedMessage();
            url = getAttachmentUrl(referenced).orElseGet(() -> event.getMessage().getReferencedMessage().getContentRaw());
        } else if (!event.getMessage().getAttachments().isEmpty()) {
            url = getAttachmentUrl(event.getMessage()).orElse("");
        } else {
            event.getMessage().reply("Ссылка на гифку не указана.").queue();
            return;
        }

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            event.getMessage().reply("Указана неверная ссылка.").queue();
            return;
        }

        if (url.contains("//tenor.com/")) {
            try {
                String html = IOUtils.toString(new URL(url), StandardCharsets.UTF_8);
                url = html.split("meta itemProp=\"contentUrl\" content=\"", 2)[1].split("\"", 2)[0];
            } catch (IOException e) {
                return;
            }
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new URL(url).openStream())) {
            List<String> lines = Arrays.asList(text.split("\n"));
            Collections.reverse(lines);

            grabber.start();

            BufferedImage textImage = new BufferedImage(grabber.getImageWidth(), grabber.getImageHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D textGraphics = textImage.createGraphics();
            textGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            textGraphics.setFont(FONT);
            textGraphics.setColor(Color.WHITE);
            textGraphics.getFontMetrics().getHeight();

            Rectangle2D bounds = null;
            for (String line : lines) {
                Rectangle2D currentBounds = textGraphics.getFontMetrics().getStringBounds(line, textGraphics);
                if (bounds == null) {
                    bounds = currentBounds;
                    continue;
                }

                if (currentBounds.getWidth() > bounds.getWidth()) {
                    bounds = currentBounds;
                }
            }

            assert bounds != null;
            Font font = FONT.deriveFont(Math.min(FONT.getSize2D() * textImage.getWidth() / (float) bounds.getWidth(), 40.0F));
            textGraphics.setFont(font);
            bounds = textGraphics.getFontMetrics().getStringBounds(text, textGraphics);
            AffineTransform transform = textGraphics.getTransform();
            transform.translate(0, textImage.getHeight() + (int) (bounds.getHeight() / 2.0D));
            for (String line : lines) {
                bounds = textGraphics.getFontMetrics().getStringBounds(line, textGraphics);
                int horizontalCenter = textImage.getWidth() / 2 - (int) bounds.getWidth() / 2;
                transform.translate(horizontalCenter, -bounds.getHeight());
                textGraphics.setTransform(transform);
                textGraphics.setColor(Color.BLACK);
                FontRenderContext frc = textGraphics.getFontRenderContext();
                TextLayout tl = new TextLayout(line, font, frc);
                Shape shape = tl.getOutline(null);
                textGraphics.setStroke(new BasicStroke(2.0F));
                textGraphics.draw(shape);
                textGraphics.setColor(Color.WHITE);
                textGraphics.fill(shape);
                transform.translate(-horizontalCenter, 0);
            }

            textGraphics.dispose();

            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(outputStream);
            encoder.setRepeat(0);
            encoder.setSize(textImage.getWidth(), textImage.getHeight());
            encoder.setDelay((int) (1000 / grabber.getFrameRate()));

            Frame frame;
            while ((frame = grabber.grabFrame(false, true, true, false, false)) != null) {
                BufferedImage image = FRAME_CONVERTER.convert(frame);
                Graphics2D graphics = image.createGraphics();
                graphics.drawImage(textImage, 0, 0, null);
                graphics.dispose();
                encoder.addFrame(image);
            }

            grabber.stop();
            encoder.finish();

            byte[] bytes = outputStream.toByteArray();
            if (bytes.length < 8000000) {
                event.getMessage().replyFiles(FileUpload.fromData(bytes, "monke.gif")).queue();
            } else {
                String filename = System.nanoTime() + ".gif";
                IOUtils.write(bytes, new FileOutputStream("data/monke/" + filename));
                event.getMessage().reply("https://api.jnngl.me/monke/" + filename).queue();
            }
        } catch (IOException e) {
            event.getMessage().reply("Не получилось добавить текст на говногифку: " + e.getMessage()).queue();
        }
    }
}
