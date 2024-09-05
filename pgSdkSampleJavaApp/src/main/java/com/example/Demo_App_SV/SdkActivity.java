package com.example.Demo_App_SV;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.proglove.sdk.*;
import de.proglove.sdk.button.ButtonPress;
import de.proglove.sdk.button.IButtonOutput;
import de.proglove.sdk.button.IPgTriggersUnblockedOutput;
import de.proglove.sdk.commands.PgCommand;
import de.proglove.sdk.commands.PgCommandParams;
import de.proglove.sdk.display.*;
import de.proglove.sdk.scanner.*;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class SdkActivity extends AppCompatActivity implements IServiceOutput, IScannerOutput, IButtonOutput,
        IPgTriggersUnblockedOutput, IDisplayOutput{

    private static final String TAG = SdkActivity.class.getSimpleName();
    static protected boolean firstOfItsKind = true;
    public static final Logger logger = Logger.getLogger(TAG);
    public static final IPgManager pgManager = new PgManager(logger, Executors.newCachedThreadPool()); // Tentar retirar static dessas duas variaveis

    //Identificacao feedback para luva
    protected PgPredefinedFeedback FeedbackID = PgPredefinedFeedback.SUCCESS; // feedback padrao

    // Connection
    private Button serviceConnectBtn;
    private Button scannerConnectBtn;

    //Data Insertion (Product codes, number of each product, destiny codes (places))
    private Button loadProductsBtn;
    private Button startReadingsBtn;

    //vector for product codes, quantities and destination codes
    private static ArrayList<String> prod_cod_loaded = new ArrayList<String>();
    private static ArrayList<String> qtd_prod_loaded = new ArrayList<String>();
    private static ArrayList<String> dest_prod_loaded = new ArrayList<String>();
    private static ArrayList<String> read_cod_loaded = new ArrayList<String>();

    // hora de inicio do sistema
    public static long initTime = 0;
    public static long timeOfOperation = 0;

    public static String read_code = ""; // leituras

    //variables related to the receive of data from load screen
    private static final int CODE_LOAD = 42;
    private static final int CODE_START = 43;

    private static final int RESULT_OK_LOAD = 1;

    // verificar se tem dados nos vetores
    private static boolean data_filled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        if (!isTaskRoot() && !firstOfItsKind) {
            finish();
            return;
        }

        setContentView(R.layout.activity_sdk_sample);

        initViews(); // Load elements in the screen (and integrate)
        initClickListeners(); // Configure the actions performed when the buttons are pressed

        updateButtonStates();

        pgManager.subscribeToServiceEvents(this);
        pgManager.subscribeToScans(this);
        pgManager.subscribeToDisplayEvents(this);
        pgManager.subscribeToButtonPresses(this);
        pgManager.subscribeToPgTriggersUnblocked(this);

        pgManager.ensureConnectionToService(getApplicationContext()); // Inicia a conexão automaticamente
    }

    @Override
    protected void onDestroy(){
        pgManager.unsubscribeFromServiceEvents(this);
        pgManager.unsubscribeFromScans(this);
        pgManager.unsubscribeFromDisplayEvents(this);
        pgManager.unsubscribeFromButtonPresses(this);
        pgManager.unsubscribeFromPgTriggersUnblocked(this);

        super.onDestroy();
    }

    /*
     * IServiceOutput Implementation:
     */
    @Override
    public void onServiceConnected(){
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }

    @Override
    public void onServiceDisconnected(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IServiceOutput Implementation
     */

    /*
     * IScannerOutput Implementation:
     */

    @Override
    public void onBarcodeScanned(@NonNull final BarcodeScanResults barcodeScanResults){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                read_code = barcodeScanResults.getBarcodeContent();
                EventBus.getDefault().post(new Message(read_code));
            }
        });
    }

    @Override
    public void onScannerConnected(){
        // Buttons already updated in #onScannerStateChanged
    }

    @Override
    public void onScannerDisconnected(){
        // Buttons already updated in #onScannerStateChanged
    }

    @Override
    public void onScannerStateChanged(@NonNull ConnectionStatus connectionStatus){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IScannerOutput Implementation
     */

    /*
     * IButtonOutput Implementation:
     */
    @Override
    public void onButtonPressed(@NonNull final ButtonPress buttonPress){
        //
    }
    /*
     * End of IButtonOutput Implementation
     */

    /*
     * IDisplayOutput Implementation:
     */
    @Override
    public void onDisplayConnected(){
        // Buttons already updated in #onDisplayStateChanged
    }

    @Override
    public void onDisplayDisconnected(){
        // Buttons already updated in #onDisplayStateChanged
    }

    @Override
    public void onDisplayStateChanged(@NonNull ConnectionStatus connectionStatus){
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                updateButtonStates();
            }
        });
    }
    /*
     * End of IDisplayOutput Implementation
     */

    /*
     * IPgTriggersUnblockedOutput Implementation:
     */

    @Override
    public void onPgTriggersUnblocked(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        SdkActivity.this.getApplicationContext(),
                        "Trigger unblocked",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }
    /*
     * End of IPgTriggersUnblockedOutput Implementation
     */

    private void initViews(){
        serviceConnectBtn = findViewById(R.id.serviceConnectBtn);
        scannerConnectBtn = findViewById(R.id.connectScannerRegularBtn);
        loadProductsBtn = findViewById(R.id.loadProductsBtn);
        startReadingsBtn = findViewById(R.id.startReadingsBtn);
    }

    private void initClickListeners(){
        // Connect to the PG Service
        serviceConnectBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                pgManager.ensureConnectionToService(getApplicationContext());
            }
        });

        // Pair scanner
        scannerConnectBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onScannerConnectBtnClick(false);
            }
        });

        loadProductsBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent_load_data = new Intent(SdkActivity.this, LoadData.class);
                intent_load_data.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                startActivityForResult(intent_load_data, CODE_LOAD);
            }
        });

        startReadingsBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent_start_readings = new Intent(SdkActivity.this, StartReadings.class);
                intent_start_readings.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

                update_screen(0,0,Integer.parseInt(qtd_prod_loaded.get(0))); // exibir dados na tela da primeira leitura
                startActivityForResult(intent_start_readings, CODE_START);
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){ // backing from the other screens (load or readings)
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == CODE_LOAD && resultCode == RESULT_OK_LOAD){
            update_screen(5,0,Integer.parseInt(qtd_prod_loaded.get(0))); // "Pronto para iniciar leituras!"
            data_filled = true;
        }else if(requestCode == CODE_START){
            update_screen(-1,-1,-1); // "Preencha lista de itens!" -> Index = -1 (não sera utilizado)
            data_filled = false;
        }

        updateButtonStates(); //call this function to update the state of 'start readings' button
    }

    private void onScannerConnectBtnClick(boolean isPinnedMode){

        if(!pgManager.isConnectedToService()){
            String msg = getString(R.string.connect_to_service_first);
            showMessage(msg, false);
            return;
        }

        if(pgManager.isConnectedToScanner()){
            pgManager.disconnectScanner();
        }else if (isPinnedMode){
            pgManager.startPairingFromPinnedActivity(this);
        }else{
            pgManager.startPairing();
        }
    }

    private void updateButtonStates(){

        updateServiceButtons();
        updateScannerButtons();
        updateStartButton();


    }

    private void updateServiceButtons(){
        if(pgManager.isConnectedToService()){
            serviceConnectBtn.setEnabled(false);
            serviceConnectBtn.setText("Serviço conectado");
        }else{
            serviceConnectBtn.setEnabled(true);
            serviceConnectBtn.setText("Conectar serviço");
        }
    }

    private void updateScannerButtons() {
        if(pgManager.isConnectedToService() && pgManager.isConnectedToScanner()){
            scannerConnectBtn.setText("Desparear scanner");
        }else{
            scannerConnectBtn.setText(R.string.pair_scanner);
        }
    }

    private void updateStartButton(){
        if(pgManager.isConnectedToService() && pgManager.isConnectedToScanner() && data_filled){
            startReadingsBtn.setEnabled(true);
        }else{
            if(pgManager.isConnectedToService() && pgManager.isConnectedToScanner()){
                update_screen(-1,-1,-1);
            }
            startReadingsBtn.setEnabled(false);
        }
    }

    protected void showMessage(final String msg, final boolean isError){
        if(isError){
            Log.e(TAG, msg);
        }else{
            Log.d(TAG, msg);
        }
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    protected void update_screen(int option,int index,int qtd_atual){
        switch (option){
            case (0): // textos de codigos de produto e quantidade
                PgTemplateField[] data_prod_codes = {
                        new PgTemplateField(1, "Local produto", dest_prod_loaded.get(index)),
                        new PgTemplateField(2, "Código produto", prod_cod_loaded.get(index)),
                        new PgTemplateField(3, "Qtde.", Integer.toString(qtd_atual).concat("/").concat(qtd_prod_loaded.get(index)))
                };
                PgScreenData screenData_prod_codes = new PgScreenData("PG3", data_prod_codes, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_prod_codes);
                break;
            case (1): // textos de codigos de destino
                PgTemplateField[] data_dest_codes = {
                        new PgTemplateField(1, "Código Destino", dest_prod_loaded.get(index))};
                PgScreenData screenData_dest_codes = new PgScreenData("PG1", data_dest_codes, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_dest_codes);
                break;
            case (2): // codigo produto errado
                PgTemplateField[] data_notif_wrong = {
                        new PgTemplateField(1, "", "Código produto incorreto!")};
                PgScreenData screenData_notif_wrong = new PgScreenData("PG1E", data_notif_wrong, RefreshType.PARTIAL_REFRESH,300);
                sendScreen(screenData_notif_wrong);
                break;
            case(3): // codigo destino errado
                PgTemplateField[] data_notif_wrong_dest = {
                        new PgTemplateField(1, "", "Código destino incorreto!")};
                PgScreenData screenData_notif_wrong_dest = new PgScreenData("PG1E", data_notif_wrong_dest, RefreshType.PARTIAL_REFRESH,300);
                sendScreen(screenData_notif_wrong_dest);
                break;
            case (4): // trabalho fechado
                long min = (long) (timeOfOperation / (1000*60));
                long sec = (long) ((timeOfOperation%(1000*60))/1000);
                String duracao = String.valueOf(min) + "m " + String.valueOf(sec) + "s";

                PgTemplateField[] data_job_done = {
                        new PgTemplateField(1, "", duracao),
                        new PgTemplateField(2,"","Trigger duplo para repetir!")};
                PgScreenData screenData_job_done = new PgScreenData("PG2C", data_job_done, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_job_done);
                break;
            case (5): // Ir para tela start
                PgTemplateField[] data_start = {
                        new PgTemplateField(1, "", "Pronto para iniciar leituras!")};
                PgScreenData screenData_start = new PgScreenData("PG1A", data_start, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_start);
                break;
            case (6): // codigo correto
                PgTemplateField[] data_notif_right = {
                        new PgTemplateField(1, "", "Código correto!"),
                        new PgTemplateField(2,"",read_code)};
                PgScreenData screenData_notif_right = new PgScreenData("PG2C", data_notif_right, RefreshType.PARTIAL_REFRESH,300);
                sendScreen(screenData_notif_right);
                break;
            case (7): // textos de codigos de produto e quantidade
                PgTemplateField[] data_prod_codes_update = {
                        new PgTemplateField(2, "Qtde.", Integer.toString(qtd_atual))};
                PgScreenData screenData_prod_codes_update = new PgScreenData("PG2", data_prod_codes_update, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_prod_codes_update);
                break;
            case (-1): // voltar tela inicial
                PgTemplateField[] data_begin = {
                        new PgTemplateField(1, "", "Preencha lista de itens!")};
                PgScreenData screenData_begin = new PgScreenData("PG1A", data_begin, RefreshType.PARTIAL_REFRESH); // Partial refresh para atualizacao ser mais rapida
                sendScreen(screenData_begin);
                break;
        }
    }

    protected void sendScreen(PgScreenData screenData){
        if (pgManager.isConnectedToService() && pgManager.isConnectedToDisplay()){
            pgManager.setScreen(screenData, new IPgSetScreenCallback(){
                @Override
                public void onSuccess(){

                }

                @Override
                public void onError(@NonNull final PgError pgError){
                    String msg = "Setting the screen failed. Error: " + pgError;
                    showMessage(msg, true);
                }
            });
        }
    }

    protected void triggerFeedback(){
        // Creating new PgCommandParams setting the queueing behaviour
        PgCommandParams params = new PgCommandParams(false);
        // Wrapping the feedback data in a PgCommand with the PgCommandData

        PgCommand<PgPredefinedFeedback> feedbackCommand = FeedbackID.toCommand(params);
        pgManager.triggerFeedback(feedbackCommand, new IPgFeedbackCallback(){
            @Override
            public void onSuccess() {
                Log.d(TAG, "Feedback successfully played.");
            }

            @Override
            public void onError(@NonNull PgError pgError) { Log.d(TAG,"Feedback not successfully played."); }
        });
    }

    protected void changeScannerConfig(final boolean isDefault){

        PgScannerConfig config = new PgScannerConfig(isDefault);
        pgManager.setScannerConfig(config, new IPgScannerConfigCallback(){
            @Override
            public void onScannerConfigSuccess(@NonNull PgScannerConfig pgScannerConfig){
                Log.d(TAG, "Successfully updated config on scanner");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }

            @Override
            public void onError(@NonNull PgError pgError) {
                final String msg = "Could not set config on scanner: " + pgError;
                showMessage(msg, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    public static ArrayList<String> getProd_cod_loaded(){
        return prod_cod_loaded;
    }

    public static ArrayList<String> getQtd_prod_loaded(){
        return qtd_prod_loaded;
    }

    public static ArrayList<String> getDest_prod_loaded(){
        return dest_prod_loaded;
    }

    public static ArrayList<String> getRead_code_loaded(){
        return read_cod_loaded;
    }
}