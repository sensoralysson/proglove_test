package com.example.Demo_App_SV;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.proglove.sdk.button.ButtonPress;
import de.proglove.sdk.scanner.BarcodeScanResults;
import de.proglove.sdk.scanner.PgPredefinedFeedback;

// Herda da classe principal, para ter acesso as funcionalidades implementadas para a luva em SDKActivity
public class StartReadings extends SdkActivity{

    // Textviews que serao preenchidos com dados
    private TextView ExibeCodProd;
    private TextView ExibeQtdProd;
    private TextView ExibeDestProd;

    // Codigo lido
    private String codeReading;

    // Arraylist with the data from load screen
    private ArrayList<String> Code_product;
    private ArrayList<String> Read_code_product;
    private ArrayList<String> Qtde_product;
    private ArrayList<String> Destin_product;


    public static int index = 0; // primeiro item do vetor
    public static int inicial_size = 0; // tamanho inicial dos vetores

    public static int n_codigos_restantes = 0;
    public static boolean new_product_code = true;

    // booleanos para controle das leituras -> isOver - Indica fim ou nao do processo, isOnTimeDestiny indica se esta na vez da leitura do dest. codigo
    public static boolean isOver = false;
    public static boolean isOnTimeDestiny = false;

    // Deixar um tempo para atualizar a tela (handler para atualizacao)
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


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        firstOfItsKind = false;
        setContentView(R.layout.activity_start_readings);
        stopService();

        Code_product = SdkActivity.getProd_cod_loaded();
        Read_code_product = SdkActivity.getRead_code_loaded();
        Qtde_product = SdkActivity.getQtd_prod_loaded();
        Destin_product = SdkActivity.getDest_prod_loaded();

        initTime = SystemClock.elapsedRealtime();
        timeOfOperation = 0;

        // zera variaveis estaticas
        index = 0;
        isOver = false;
        n_codigos_restantes = Integer.parseInt(Qtde_product.get(index));
        new_product_code = true;
        isOnTimeDestiny = false;

        inicial_size = Code_product.size();

        ExibeCodProd = (TextView) findViewById(R.id.ExibeCodProd);
        ExibeQtdProd = (TextView) findViewById(R.id.ExibeQtdProd);
        ExibeDestProd = (TextView) findViewById(R.id.ExibeDestProd);

        ExibeCodProd.setText((CharSequence) Code_product.get(index));
        ExibeQtdProd.setText((CharSequence) Qtde_product.get(index));
        ExibeDestProd.setText((CharSequence) Destin_product.get(index));

        changeScannerConfig(false); // Retirando luz verde após cada scan (padrao)
    }



    public void startService() {
        Intent serviceIntent = new Intent(this, SdkService.class);
        serviceIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        MyParcelable parcelable = new MyParcelable(Code_product,Read_code_product,Destin_product,Qtde_product);

        serviceIntent.putExtra("serviceMsg", "Clique aqui para retornar para o aplicativo.");
        serviceIntent.putExtra("info",parcelable);

        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService() {
        Intent serviceIntent = new Intent(this, SdkService.class);
        this.stopService(serviceIntent);

    }

    @Override
    public void onStart(){
        super.onStart();
        stopService();
        EventBus.getDefault().register(this);

        if(isOver){
            clean_textviews();
        }else{
            update_textviews(n_codigos_restantes);
        }


    }

    @Override
    public void onStop(){
        super.onStop();

        EventBus.getDefault().unregister(this);

        if(!isFinishing()) {
            startService();
        }else{
            stopService();
        }

    }


    @Override
    public void onDestroy(){
        changeScannerConfig(true); // Retornando feedback padrao
        firstOfItsKind = true;

        // Limpando vetores de codigos produto e destino, e quantidades
        SdkActivity.getProd_cod_loaded().clear();
        SdkActivity.getQtd_prod_loaded().clear();
        SdkActivity.getDest_prod_loaded().clear();
        SdkActivity.getRead_code_loaded().clear();

        super.onDestroy();
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

    @Override
    public void onButtonPressed(@NonNull final ButtonPress buttonPress){ // Refresh dos dados
        if(isOver){ // Repete loop por duas vezes
            index = 0;

            initTime = SystemClock.elapsedRealtime();
            timeOfOperation = 0;

            isOver = false; // Reinicio do ciclo
            update_textviews(Integer.parseInt(Qtde_product.get(0)));
            update_screen(0, 0, Integer.parseInt(Qtde_product.get(0))); // atualizando tela para cod prod e qtde
            new_product_code = true;

        }
    }

    @Override
    public void onBarcodeScanned(@NonNull final BarcodeScanResults barcodeScanResults){
        // Empty implementation to avoid double readings
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
                update_textviews(n_codigos_restantes);//Para atualizliar a qtde de codigos dos

                make_toast(1); // Código produto correto
            } else {
                update_screen(2, index, n_codigos_restantes); // codigo produto incorreto
                make_toast(0); // Código inválido
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

                    update_textviews(n_codigos_restantes);
                    make_toast(2); // Código destino correto
                } else {
                    isOver = true; // Impedir que atualização da tela aconteça sobre o "JOB DONE"
                    make_toast(3);
                    timeOfOperation = SystemClock.elapsedRealtime() - initTime;

                    update_screen(4, index,n_codigos_restantes); // trabalho feito
                    clean_textviews();
                    FeedbackID = PgPredefinedFeedback.SUCCESS;
                }
            } else {
                update_screen(3, index,n_codigos_restantes); // codigo produto incorreto
                make_toast(0); // Código inválido

                FeedbackID = PgPredefinedFeedback.ERROR; // sinal vermelho
            }
        }
    }

    private void make_toast(int icon){
        int imgID = 0;

        if(icon == 0){ // icone leitura errada
            imgID = R.drawable.icone_fail;
        }

        if(icon == 1 || icon == 2 || icon == 3){ // icone ok
            imgID = R.drawable.icone_ok;
        }

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        ImageView image = (ImageView) layout.findViewById(R.id.image);
        image.setImageResource(imgID);
        TextView text = (TextView) layout.findViewById(R.id.text);

        if(icon == 0){
            text.setText("Código incorreto! Leia novamente!");
        }else if(icon == 1){
            text.setText("Código de produto correto!");
        }else if(icon == 2){
            text.setText("Código de destino correto!");
        }else if(icon == 3){
            text.setText("Trabalho finalizado! Trigger duplo para reiniciar");
        }

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 50);

        if(icon != 3){
            toast.setDuration(Toast.LENGTH_SHORT);
        }else{
            toast.setDuration(Toast.LENGTH_LONG); // Caso seja o final (para mensagem aparecer mais tempo na tela)
        }

        toast.setView(layout);
        toast.show();
    }

    private void update_textviews(int qtde_atual){
        ExibeCodProd.setText((CharSequence) Code_product.get(index));
        ExibeQtdProd.setText(Integer.toString(qtde_atual));
        ExibeDestProd.setText((CharSequence) Destin_product.get(index));
    }

    private void clean_textviews(){
        ExibeCodProd.setText("");
        ExibeQtdProd.setText("");
        ExibeDestProd.setText("");
    }
}