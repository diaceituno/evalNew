package viewControl;

import java.security.GeneralSecurityException;
import java.util.Optional;

import javax.net.ssl.SSLSocketFactory;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

import core.Configuration;
import core.MainController;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class LoginControl{

	private Stage stage;
	private Scene scene;
	private LDAPConnection ldapCon;
	
	@FXML
	private Button loginBtn;
	@FXML
	private TextField usrField;
	@FXML
	private PasswordField pwdField;
	
	public LoginControl(){
			
		/*Setting up Scene*/
		AnchorPane pane = (AnchorPane) MainController.getIOControl().loadPane("loginView.fxml", this);
		scene = new Scene(pane);
		scene.setOnKeyPressed(e -> {
			if(e.getCode() == KeyCode.ENTER) {
				loginAction();
			}
			if(e.getCode() == KeyCode.CONTROL) {
				loginSuccess();
			}
		});
		
		/*Setting up stage*/
		stage = new Stage();
		stage.setTitle("Anmeldung");
		stage.setResizable(false);
		stage.setScene(scene);
		
		/*Setting changeListenes for TextField*/
		usrField.textProperty().addListener((ob,ov,nv) -> {
			if(!nv.trim().isEmpty()) {
				loginBtn.setDisable(false);
			}else {
				loginBtn.setDisable(true);
			}
		});
	}
	
	@FXML 
	private void loginAction() {
		
		SSLUtil util = new SSLUtil(new TrustAllTrustManager());
		SSLSocketFactory fac = null;
		try {
			 fac = util.createSSLSocketFactory();
		} catch (GeneralSecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		/*Configuring LDAP Connection*/
		Configuration config = MainController.getConfiguration();
		LDAPConnectionOptions opts = new LDAPConnectionOptions();
		opts.setConnectTimeoutMillis(5000);
		ldapCon = new LDAPConnection();
		ldapCon.setConnectionOptions(opts);
		
		if(!ldapCon.isConnected()) {
			
			boolean conEstablished = false;
			Alert alert;
			while(!conEstablished) {
				
				config = MainController.getConfiguration();
				try {
					
					ldapCon.connect(config.getLDAPServer(), config.getLDAPPort());
					conEstablished = ldapCon.isConnected();
					System.out.println(ldapCon.isConnected());
				} catch (LDAPException e) {
					e.printStackTrace();
					
					alert = new Alert(AlertType.CONFIRMATION);
					alert.initOwner(stage);
					alert.setTitle("Konfigurationsfehler");
					alert.setHeaderText(null);
					alert.setContentText("LDAP Server konne nicht erreicht werden.\nKonfiguration �ndern?");
					Optional<ButtonType> result = alert.showAndWait();
					if(result.get() == ButtonType.OK) {
						MainController.configScene();
						return;
					}else {
						MainController.failure(null, false);
					}
				}
			}
		}
		
		if(!usrField.getText().isEmpty()) {
			
			String usrName = usrField.getText().endsWith(config.getLDAPServer()) 
							? usrField.getText() 
							: usrField.getText() + "@" + config.getLDAPServer();
			SimpleBindRequest sBRequest = new SimpleBindRequest(usrName, pwdField.getText());
			try {
				ldapCon.bind(sBRequest);
				loginSuccess();
			} catch (LDAPException e) {
				e.printStackTrace();
				loginFail();
			}
		}
	}
	
	private void loginFail() {
		
		Alert alert = new Alert(AlertType.ERROR);
		alert.initOwner(stage);
		alert.setTitle("Anmeldung fehlgeschlagen");
		alert.setHeaderText(null);
		alert.setContentText("Anmeldung fehlgeschlagen!\nBenutzername oder Passwort falsch");
		alert.showAndWait();
	}
	
	private void loginSuccess() {
		MainController.setLoginStatus(true);
		stage.close();
	}
	
	public void show() {
		stage.showAndWait();
	}
	
	

}
 