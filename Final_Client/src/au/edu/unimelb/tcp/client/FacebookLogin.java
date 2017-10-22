package au.edu.unimelb.tcp.client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineParser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.PopupFeatures;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebHistory.Entry;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import netscape.javascript.JSObject;

public class FacebookLogin extends Application {
    public static void main(String args[]) {

        FacebookLogin.createApplication(null);

    }

    public static void createApplication(String args[]) {
        Application.launch(args);

    }

    @Override
    public void start(Stage stage) throws Exception {
        WebView my = new WebView();
        WebEngine engine = my.getEngine();
        engine.load(
                "https://www.facebook.com/v2.8/dialog/oauth?client_id=353901168276665&redirect_uri=https://www.facebook.com/connect/login_success.html&response_type=token&scope=public_profile ");

        engine.getLoadWorker().stateProperty()
                .addListener((observable, oldValue, newValue) -> {
                    if (Worker.State.SUCCEEDED.equals(newValue)) {
                   //     System.out.println(engine.getLocation());
                        if (engine.getLocation().startsWith(
                                "https://www.facebook.com/connect/login_success.html")){
                            // processLogin(fxPanel,webEngine.getLocation());
                          //  System.out.println(engine.getLocation());
                            ClientThread handlelogin=new ClientThread(engine.getLocation());
                            handlelogin.start();
                            Platform.exit();

                        // location.setText(engine.getLocation());
                        }
                    }
                });
        VBox root = new VBox();
        root.getChildren().addAll(my);

        Scene scene = new Scene(root, 500, 200);
        stage.setScene(scene);
        stage.show();
    }
}