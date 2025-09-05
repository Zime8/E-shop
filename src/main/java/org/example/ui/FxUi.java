package org.example.ui;// package org.example.ui;

import javafx.scene.Node;
import javafx.stage.Stage;

public final class FxUi {
    private FxUi() {}

    public static void closeWindow(Stage stage, Node anyNode) {
        if (stage != null) { stage.close(); return; }
        if (anyNode != null && anyNode.getScene() != null) {
            Stage s = (Stage) anyNode.getScene().getWindow();
            if (s != null) s.close();
        }
    }
}
