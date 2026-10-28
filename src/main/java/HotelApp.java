import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class HotelApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(HotelApp.class.getResource("/HotelView.fxml"));
        stage.setTitle("Gestión de Hotel");
        stage.setScene(new Scene(root, 1000, 700));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
