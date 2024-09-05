package com.example.Demo_App_SV;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.appcompat.app.AppCompatActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public class LoadData extends AppCompatActivity{

    private static final int OK_LOAD = 1;

    private EditText edtProduct, edtReadCode, edtQty, edtDestiny;
    private Button BtnAdd, BtnSave, BtnLoadTable;
    private ImageButton BtnTrash;

    private TableLayout table;


    private String code_received;

    // Configuraccoes de leitura de arquivo
    public static final String separador = ";";

    // Strings lidas do input text na tela de load
    private String Cod_Prod;
    private String Qtd_Prod;
    private String Cod_Dest;
    private String Cod_Read;

    // Arrays from data in load
    ArrayList<String> cod_prod_inserted;
    ArrayList<String> qtd_prod_inserted;
    ArrayList<String> dest_prod_inserted;
    ArrayList<String> cod_read_inserted;

    private int size_vectors = 0;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_data);

        //pegando valores dos vetores da classe main
        cod_prod_inserted = SdkActivity.getProd_cod_loaded();
        qtd_prod_inserted = SdkActivity.getQtd_prod_loaded();
        dest_prod_inserted = SdkActivity.getDest_prod_loaded();
        cod_read_inserted = SdkActivity.getRead_code_loaded();

        size_vectors = cod_prod_inserted.size();

        edtProduct = (EditText) findViewById(R.id.edtProduct);
        edtQty = (EditText) findViewById(R.id.edtQty);
        edtDestiny = (EditText) findViewById(R.id.edtDestiny);
        edtReadCode = (EditText) findViewById(R.id.edtReadCode);

        BtnAdd = (Button) findViewById(R.id.BtnAdd);
        BtnSave = (Button) findViewById(R.id.BtnSave);
        BtnLoadTable = (Button) findViewById(R.id.BntLoadTable);
        BtnTrash = (ImageButton) findViewById(R.id.BtnTrash);

        BtnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRegister(v); // adiciona item na tabela
            }
        });

        BtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cod_prod_inserted.isEmpty()){ // lista está vazia
                    return;
                }

                Intent intent = new Intent();

                setResult(OK_LOAD, intent);
                finish(); // return to main activity
            }
        });

        BtnTrash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                erase_table_entrys();
                showManualEntrys();
            }
        });

        BtnLoadTable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showFileChooser();
            }
        });

        showManualEntrys();
        table = (TableLayout) findViewById(R.id.main_table); // Configuraion of table and its header

        fill_table_line("Código Prod.","Código Leitura","Qtde.","Destino Prod."); // Adicionando cabecalho da tabela
        erase_table_entrys(); // apaga todas as entradas anteriores, deixando apenas o cabecalho

        if (!cod_prod_inserted.isEmpty() && !qtd_prod_inserted.isEmpty() && !dest_prod_inserted.isEmpty() && !cod_read_inserted.isEmpty()) {
            int ind = 0;
            while (ind < size_vectors) {
                fill_table_line(cod_prod_inserted.get(ind), cod_read_inserted.get(ind), qtd_prod_inserted.get(ind), dest_prod_inserted.get(ind));
                ind++;
            }
        }
    }

    private void showManualEntrys(){
        int color = Color.BLACK;
        boolean state = true;

        BtnAdd.setClickable(state);

        BtnAdd.setTextColor(color);
        edtReadCode.setTextColor(color);
        edtDestiny.setTextColor(color);
        edtProduct.setTextColor(color);
        edtQty.setTextColor(color);
        edtQty.setTextColor(color);

    }
    private void hideManualEntrys(){
        int color = Color.LTGRAY;
        boolean state = false;

        BtnAdd.setClickable(state);


        BtnAdd.setTextColor(color);
        edtReadCode.setTextColor(color);
        edtDestiny.setTextColor(color);
        edtProduct.setTextColor(color);
        edtQty.setTextColor(color);
        edtQty.setTextColor(color);
    }

    private static final int FILE_SELECT_CODE = 0;

    // Abrindo explorador de arquivos
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Selecione um arquivo:"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Favor instalar navegador de arquivos.",
                    Toast.LENGTH_SHORT).show();
        }
    }


    // Abrindo arquivos
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // If the user doesn't pick a file just return
        if (requestCode != FILE_SELECT_CODE || resultCode != RESULT_OK) {
            return;
        }

        Uri uri = data.getData();
        // Import the file
        String extention = getContentResolver().getType(uri);

        if(!Objects.equals(extention,"text/csv") && !Objects.equals(extention,"text/comma-separated-values")){
            //CharSequence msg = "Apenas tabelas no formato .CSV são suportadas.";
            CharSequence msg = "Apenas tabelas no formato .CSV são suportadas.";
            Toast.makeText(this,msg,Toast.LENGTH_LONG).show();
            return;
        }
        table = (TableLayout) findViewById(R.id.main_table);

        File file = null;
        FileReader fr = null;
        BufferedReader br = null;

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            br = new BufferedReader(new InputStreamReader(inputStream,"UTF-8"));

            String line = "";
            String[] tempArr;

            line = br.readLine(); // Le primeiramente cabecalho

            erase_table_entrys(); // Apaga leituras anteriores

            while ((line = br.readLine()) != null) {
                tempArr = line.split(separador);

                Cod_Prod = tempArr[0]; // Pegara o que tiver nos campos de texto (vindo do scanner ou teclado)
                Cod_Read = tempArr[1];
                Qtd_Prod = tempArr[2];
                Cod_Dest = tempArr[3];

                fill_table_line(Cod_Prod, Cod_Read, Qtd_Prod, Cod_Dest);

                // Filling positions in vectors
                cod_prod_inserted.add(size_vectors, Cod_Prod);
                qtd_prod_inserted.add(size_vectors, Qtd_Prod);
                dest_prod_inserted.add(size_vectors, Cod_Dest);
                cod_read_inserted.add(size_vectors, Cod_Read);

                size_vectors++;
            }
            br.close();

            hideManualEntrys();

        } catch (IOException e) {
            System.out.println("There was a problem: " + e);
            e.printStackTrace();

        } finally {
            try {
                br.close();
            } catch (Exception e) {
            }
        }

    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop(){
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    @Subscribe
    public void onMessage(Message event){
        code_received = event.getMessage(); // salvando leitura do scanner nessa atividade

        if(edtProduct.getText().length() == 0){
            edtProduct.setText(code_received);
        }else if(edtReadCode.getText().length() == 0){
            edtReadCode.setText(code_received);
        }else if(edtDestiny.getText().length() == 0){
            edtDestiny.setText(code_received);
        }
    }


//    @SuppressLint("ResourceType")
//    private void addTableRegister(View v, Intent intent){
//
//        Context context = getApplicationContext();
//        Uri fileUri = intent.getData();
//
//        Toast.makeText(context, fileUri.getPath(), Toast.LENGTH_LONG).show();
//
//        table = (TableLayout) findViewById(R.id.main_table);
//        fill_table_line(fileUri.getPath(), "-", "-", "-");
//    }

    @SuppressLint("ResourceType")
    private void addRegister(View v){

        Cod_Prod = edtProduct.getText().toString(); // Pegara o que tiver nos campos de texto (vindo do scanner ou teclado)
        Qtd_Prod = edtQty.getText().toString();
        Cod_Dest = edtDestiny.getText().toString();
        Cod_Read = edtReadCode.getText().toString();

        //Clear the texts typed in the fields
        edtProduct.setText("");
        edtQty.setText("");
        edtDestiny.setText("");
        edtReadCode.setText("");

        // return if the input fields are blank
        if (TextUtils.isEmpty(Cod_Prod) || TextUtils.isEmpty(Qtd_Prod) || TextUtils.isEmpty(Cod_Dest) ||
                TextUtils.isEmpty(Cod_Read)){
            return;
        }

        table = (TableLayout) findViewById(R.id.main_table);

        fill_table_line(Cod_Prod, Cod_Read, Qtd_Prod, Cod_Dest);

        // Filling positions in vectors
        cod_prod_inserted.add(size_vectors, Cod_Prod);
        qtd_prod_inserted.add(size_vectors, Qtd_Prod);
        dest_prod_inserted.add(size_vectors, Cod_Dest);
        cod_read_inserted.add(size_vectors, Cod_Read);

        size_vectors++;
    }

    private void erase_table_entrys(){
        table = (TableLayout) findViewById(R.id.main_table);
        table.removeViews(1,table.getChildCount()-1);

        cod_prod_inserted.clear();
        qtd_prod_inserted.clear();
        dest_prod_inserted.clear();
        cod_read_inserted.clear();

        size_vectors = 0;
    }

    private void fill_table_line(String prod_code, String prod_read, String qtd_prod, String dest_prod){

        TableRow tr_line = new TableRow(this); // Creating new line of the table
        tr_line.setBackgroundColor(Color.WHITE);
        tr_line.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));

        TextView label_ProdCod = new TextView(this);
        label_ProdCod.setText(prod_code);
        label_ProdCod.setTextColor(Color.BLACK);
        tr_line.addView(label_ProdCod);// add the column to the table row here

        TextView label_ReadCod = new TextView(this);
        label_ReadCod.setText(prod_read);
        label_ReadCod.setTextColor(Color.BLACK);
        tr_line.addView(label_ReadCod);// add the column to the table row here

        TextView label_QtdProd = new TextView(this);
        label_QtdProd.setText(qtd_prod); // set the text for the header
        label_QtdProd.setTextColor(Color.BLACK); // set the color
        tr_line.addView(label_QtdProd); // add the column to the table row here

        TextView label_DestProd = new TextView(this);
        label_DestProd.setText(dest_prod); // set the text for the header
        label_DestProd.setTextColor(Color.BLACK); // set the color
        tr_line.addView(label_DestProd); // add the column to the table row here

        table.addView(tr_line, new TableLayout.LayoutParams( // Add line with data (product code, product read code, quantity, destiny code)
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT));
    }
}