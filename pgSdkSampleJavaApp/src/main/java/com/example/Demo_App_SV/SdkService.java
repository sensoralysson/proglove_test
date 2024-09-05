package com.example.Demo_App_SV;

import static com.example.Demo_App_SV.StartReadings.n_codigos_restantes;
import static com.example.Demo_App_SV.StartReadings.new_product_code;
import static com.example.Demo_App_SV.StartReadings.isOnTimeDestiny;
import static com.example.Demo_App_SV.StartReadings.index;
import static com.example.Demo_App_SV.StartReadings.isOver;
import static com.example.Demo_App_SV.StartReadings.inicial_size;
import static com.example.Demo_App_SV.SdkActivity.pgManager;
import static com.example.Demo_App_SV.SdkActivity.read_code;
import static com.example.Demo_App_SV.SdkActivity.timeOfOperation;
import static com.example.Demo_App_SV.SdkActivity.initTime;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import androidx.annotation.Nullable;
import de.proglove.sdk.*;
import de.proglove.sdk.button.ButtonPress;
import de.proglove.sdk.button.IButtonOutput;
import de.proglove.sdk.button.IPgTriggersUnblockedOutput;
import de.proglove.sdk.commands.PgCommand;
import de.proglove.sdk.commands.PgCommandParams;
import de.proglove.sdk.display.*;
import de.proglove.sdk.scanner.*;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.app.PendingIntent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

// Classe de serviço, responsável pelo programa rodar em segundo plano quando necessário
// Repete funcionalidades principais sem interface com a UI
public class SdkService extends Service {

    private String codeReading;

    private ArrayList<String> Code_product;
    private ArrayList<String> Read_code_product;
    private ArrayList<String> Destin_product;
    private ArrayList<String> Qtde_product;

    protected PgPredefinedFeedback FeedbackID = PgPredefinedFeedback.SUCCESS; // feedback padrao

    private Handler handler_update_screen = new Handler();
    final Runnable r = new Runnable(){
        public void run(){
            if(isOnTimeDestiny){
                update_screen(1, index, n_codigos_restantes); // 1 - Atualizacao para tela cod destino
            }else{
                update_screen(0, index, n_codigos_restantes);
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Override
    public void onCreate() {
        super.onCreate();


    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String serviceMsg = intent.getStringExtra("serviceMsg");
        MyParcelable parcelable = (MyParcelable) intent.getParcelableExtra("info");

        assert parcelable != null;

        Code_product = parcelable.Code_product;
        Read_code_product= parcelable.Read_code_product;
        Destin_product= parcelable.Destin_product;
        Qtde_product= parcelable.Qtde_product;

        EventBus.getDefault().register(this);

        createNotificationChannel();

        Intent notificationIntent = new Intent(this, StartReadings.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Leitura de ProGlove Demo em andamento...")
                .setContentText(serviceMsg)
                .setSmallIcon(R.drawable.sv_icon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);


        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        handler_update_screen.removeCallbacks(r);

    }

    protected void update_screen(int option,int index,int qtd_atual){
        switch (option){
            case (0): // textos de codigos de produto e quantidade
                PgTemplateField[] data_prod_codes = {
                        new PgTemplateField(1, "Local produto", Destin_product.get(index)),
                        new PgTemplateField(2, "Código produto", Code_product.get(index)),
                        new PgTemplateField(3, "Qtde.", Integer.toString(qtd_atual).concat("/").concat(Qtde_product.get(index)))
                };
                PgScreenData screenData_prod_codes = new PgScreenData("PG3", data_prod_codes, RefreshType.PARTIAL_REFRESH);
                sendScreen(screenData_prod_codes);
                break;
            case (1): // textos de codigos de destino
                PgTemplateField[] data_dest_codes = {
                        new PgTemplateField(1, "Código Destino", Destin_product.get(index))};
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

    @Subscribe
    public void onMessage(Message event){
        codeReading = event.getMessage(); // salvando leitura do scanner nessa atividade
        check_reading(); // verificar leitura
        if (!isOver){ // Se ainda nao chegou ao fim e se nao eh novo codigo de produto
            handler_update_screen.postDelayed(r,10); // atualizar a tela depois de 10 ms da chamada
        }
        triggerFeedback();
    }

    @Subscribe
    public void onButtonPressed(@NonNull final ButtonPress buttonPress){ // Refresh dos dados
        if(isOver){ // Repete loop por duas vezes
            index = 0;

            initTime = SystemClock.elapsedRealtime();
            timeOfOperation = 0;

            isOver = false; // Reinicio do ciclo
            update_screen(0, 0,Integer.parseInt(Qtde_product.get(0))); // atualizando tela para cod prod e qtde
            new_product_code = true;

        }
    }

    private void check_reading(){ //Verifica codigo lido e compara com codigo de leitura

        if(new_product_code) {
            n_codigos_restantes = Integer.parseInt(Qtde_product.get(index)); // numero inicial de produtos a serem lidos
        }

        if(isOver){
            FeedbackID = PgPredefinedFeedback.SPECIAL_1;

        }else if(n_codigos_restantes >= 1) {
            if (codeReading.equals(Read_code_product.get(index))) { // código produto

                if (n_codigos_restantes > 1) {
                    FeedbackID = PgPredefinedFeedback.SUCCESS; // sinal verde
                } else {
                    FeedbackID = PgPredefinedFeedback.SPECIAL_1; // Sinal amarelo de alerta
                    isOnTimeDestiny = true; // Proxima atualizacao de tela eh para a de destino
                }
                n_codigos_restantes--;
                new_product_code = false;

            } else {
                update_screen(2, index, n_codigos_restantes); // codigo produto incorreto
                FeedbackID = PgPredefinedFeedback.ERROR; // sinal vermelho
            }

        }else{ // checagem do destino (codigo correto ou nao)
            if(codeReading.equals(Destin_product.get(index))) { // código destino
                isOnTimeDestiny = false; // setando de volta para false, para novo produto

                if (index + 1 < inicial_size) {
                    new_product_code = true;
                    index++;
                    n_codigos_restantes = Integer.parseInt(Qtde_product.get(index));
                    FeedbackID = PgPredefinedFeedback.SPECIAL_1;

                } else { // FIXME sempre entrando nessa condição, mesmo quando ainda existem produtos
                    isOver = true; // Impedir que atualização da tela aconteça sobre o "JOB DONE"
                    timeOfOperation = SystemClock.elapsedRealtime() - initTime;

                    update_screen(4, index,n_codigos_restantes); // trabalho feito
                    FeedbackID = PgPredefinedFeedback.SUCCESS;
                }
            } else {
                update_screen(3, index,n_codigos_restantes); // codigo produto incorreto

                FeedbackID = PgPredefinedFeedback.ERROR; // sinal vermelho
            }
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

            }

            @Override
            public void onError(@NonNull PgError pgError) {

            }
        });
    }

}
