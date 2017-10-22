package au.edu.unimelb.tcp.client;
import java.awt.Dimension;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.jayway.jsonpath.JsonPath;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.json.simple.JSONObject;

public class Moses {
    private  static JFrame frame;

    /* Create a JFrame with a JButton and a JFXPanel containing the WebView. */
    private static void initAndShowGUI() {
        // This method is invoked on Swing thread
        frame = new JFrame("KCSnM Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(null); // do the layout manually
        final JFXPanel fxPanel = new JFXPanel();
        frame.add(fxPanel);
        frame.setVisible(true);
        fxPanel.setSize(new Dimension(800, 600));
        fxPanel.setLocation(new Point(0, 0));
        frame.getContentPane().setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setResizable(false);

        Platform.runLater(new Runnable() { // this will run initFX as JavaFX-Thread
            @Override
            public void run() {
                setFacebookLoginScene(fxPanel);
                System.out.println("Done Fx");
            }
        });
    }

    private static void initFX(final JFXPanel fxPanel) {
        Group group = new Group();
        Scene scene = new Scene(group);
        Button login=new Button("Login into KCSnM Chat using facebook");
        login.setOnAction(e->setFacebookLoginScene(fxPanel));
        group.getChildren().add(login);
        fxPanel.setScene(scene);

        
    }
    public static void setFacebookLoginScene(final JFXPanel fxPanel){
        Group groupFacebook = new Group();
        Scene facebookLogin=new Scene(groupFacebook);
        WebView webView = new WebView();

        groupFacebook.getChildren().add(webView);
        webView.setMinSize(800, 600);
        webView.setMaxSize(800, 600);

            // Obtain the webEngine to navigate
        WebEngine webEngine = webView.getEngine();
        webEngine.load("https://www.facebook.com/v2.8/dialog/oauth?client_id=353901168276665&redirect_uri=https://www.facebook.com/connect/login_success.html&response_type=token&scope=public_profile ");

        webEngine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (Worker.State.SUCCEEDED.equals(newValue)) {
                System.out.println(webEngine.getLocation());
                if(webEngine.getLocation().startsWith("https://www.facebook.com/connect/login_success.html"))
                  //  processLogin(fxPanel,webEngine.getLocation());
                    frame.dispose();
                    
               // location.setText(engine.getLocation());
            }
        });
        fxPanel.setScene(facebookLogin);
    }
    public static void processLogin(final JFXPanel fxPanel,String message){
        Group group = new Group();
        Scene scene = new Scene(group);
        if(message.contains("error=access_denied")){
        Button login=new Button("Please provide successful login to enter KCSnM Chat");
        login.setOnAction(e->setFacebookLoginScene(fxPanel));
        group.getChildren().add(login);
        }
        else if(message.contains("access_token=")){
            StringBuilder result = new StringBuilder();
            StringBuilder result2 = new StringBuilder();
           
            try {
                String endpoint="https://graph.facebook.com/debug_token?access_token=353901168276665|YxNfe88eOATUgQStUFMHbY5bGl4&input_token="+message.substring(message.indexOf("access_token=")+13,message.length());
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = rd.readLine()) != null) {
                   result.append(line);
                }
                rd.close();
                System.out.println( result.toString());
                JSONParser parse=new JSONParser();
                JSONObject jsonObj = (org.json.simple.JSONObject) parse.parse(result.toString());
                System.out.println("JSON OBject is "+jsonObj.toJSONString());
                System.out.println("JSON "+JsonPath.read(result.toString(), "$.data.is_valid"));
                if(JsonPath.read(result.toString(), "$.data.is_valid").equals(new Boolean(true))){
                    System.out.println("All good mate");
                    endpoint="https://graph.facebook.com/me?access_token="+message.substring(message.indexOf("access_token=")+13,message.length());
                    URL url2 = new URL(endpoint);
                    HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                    conn2 = (HttpURLConnection) url2.openConnection();
                    conn2.setRequestMethod("GET");
                    BufferedReader rds = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                    while ((line = rds.readLine()) != null) {
                        result2.append(line);
                    }
                    jsonObj = (org.json.simple.JSONObject) parse.parse(result2.toString());
                    System.out.println("JSON OBject is "+jsonObj.toJSONString());
                //    System.out.println("JSON "+JsonPath.read(result.toString(), "$.data.is_valid"));
                    rds.close();
                }
                
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Label label= new Label("Login Success");
            group.getChildren().add(label);
        }
        fxPanel.setScene(scene);
    }

    public static void main(String args[]){
        Moses.createGUI();
    }
    /* Start application */
    public static void createGUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initAndShowGUI();
                System.out.println("Done");
            }
        });
    }
}