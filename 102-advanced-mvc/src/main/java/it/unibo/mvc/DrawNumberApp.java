package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     * @param configPath the path of the config file.
     */
    public DrawNumberApp(final String configPath, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Configuration.Builder configBuilder = new Configuration.Builder();
        try (var reader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(configPath)))) {
            for (var line = reader.readLine(); line != null; line = reader.readLine()) {
                final String[] lineElements = line.split(":");
                final int val = Integer.parseInt(lineElements[1].trim());
                switch (lineElements[0]) {
                    case "minimum":
                        configBuilder.setMin(val);
                        break;
                    case "maximum":
                        configBuilder.setMax(val);
                        break;
                    case "attempts":
                        configBuilder.setAttempts(val);
                        break;
                    default:
                }
            }
        } catch (IOException e) {
            this.views.forEach(v -> v.displayError("Error while reading config from file " 
                + configPath + " or i can't undestand it"));
        }
        final Configuration config = configBuilder.build();
        if (config.isConsistent()) {
            this.model = new DrawNumberImpl(config);
        } else {
            this.views.forEach(v -> v.displayError("Inconsistent configuration read from file."
                + " Using default configuration instead"));
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp("config.yml",
            new DrawNumberViewImpl(),
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out));
    }

}
