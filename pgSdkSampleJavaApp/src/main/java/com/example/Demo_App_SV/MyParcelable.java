package com.example.Demo_App_SV;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.concurrent.Executors;

import de.proglove.sdk.IPgManager;
import de.proglove.sdk.PgManager;

// simple class that just has one member property as an example
public class MyParcelable implements Parcelable {
    public ArrayList<String> Code_product;
    public ArrayList<String> Read_code_product;
    public ArrayList<String> Destin_product;
    public ArrayList<String> Qtde_product;

    /**
     * Constructs a MyParcelable from values
     */
    public MyParcelable (ArrayList<String> Code_product, ArrayList<String> Read_code_product
            , ArrayList<String> Destin_product,ArrayList<String> Qtde_product) {

        this.Code_product = Code_product;
        this.Read_code_product = Read_code_product;
        this.Destin_product = Destin_product;
        this.Qtde_product = Qtde_product;
    }

    /**
     * Constructs a MyParcelable from values
     */
    public MyParcelable (Parcel parcel) {

        this.Code_product = parcel.readArrayList(null);
        this.Read_code_product = parcel.readArrayList(null);
        this.Destin_product = parcel.readArrayList(null);
        this.Qtde_product = parcel.readArrayList(null);
    }
    /* everything below here is for implementing Parcelable */

    // 99.9% of the time you can just ignore this
    @Override
    public int describeContents() {
        return 0;
    }

    // write your object's data to the passed-in Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeList(Code_product);
        dest.writeList(Read_code_product);
        dest.writeList(Destin_product);
        dest.writeList(Qtde_product);


    }

    // Method to recreate a Question from a Parcel
    public static Creator<MyParcelable> CREATOR = new Creator<MyParcelable>() {

        @Override
        public MyParcelable createFromParcel(Parcel source) {
            return new MyParcelable(source);
        }

        @Override
        public MyParcelable[] newArray(int size) {
            return new MyParcelable[size];
        }

    };

}