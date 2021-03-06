package com.example.miranlee.lifewithaac;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by sungd on 2017-05-28.
 */

public class SosActivity extends Activity implements LocationListener {
    Button addbutton;
    ListView listView;
    ListviewAdapter adapter;
    String name = null;
    String number = null;

    WarningDB warningDB;
    SQLiteDatabase db;

    //dialog
    String selectname = null;
    String selectnumber = null;
    ImageButton callbtn;
    ImageButton snsbtn;

    CustomDialog customDialog;

    //위치받아오기
    Location myLocation;
    Geocoder geoCoder;
    double lat;
    double lon;
    String add;

    ArrayList<SOS> sos = new ArrayList<SOS>();

    boolean len=true;//길게누르지않음

    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_sos);
        setTitle("sos");
        super.onCreate(savedInstanceState);


        init();
    }

    void init() {


        listView = (ListView) findViewById(R.id.warninglist);
        adapter = new ListviewAdapter(this, sos);
        listView.setAdapter(adapter);
//
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, (LocationListener) this);
        geoCoder = new Geocoder(this,Locale.KOREAN);
//

        //////여기서부터 추가된거!
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int pos, long l) {
                len = false; //길게누른것

                //    Toast.makeText(getApplicationContext(),String.valueOf(i)+"오래눌림".toString(), Toast.LENGTH_LONG).show();

                //진짜 지울건지 물어보는 다이얼로그 창
                AlertDialog.Builder builder = new AlertDialog.Builder(SosActivity.this);
                builder.setMessage("삭제하시겠습니까?")
                        .setCancelable(false)
                        .setPositiveButton("네", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //리스트뷰 포지션에 해당하는 전화번호 값 가져와서
                                SOS item =(SOS)adapter.getItem(pos);
                                selectname = item.getMname();
                                selectnumber= item.getMtel();
                                //디비에 있는거랑 비교해서 찾아서 그거 delete
                                //WarningHandler warningHandler = new WarningHandler(getApplicationContext());
                                warningDB = new WarningDB(getApplicationContext());

                                warningDB.numdelete(selectname,selectnumber);   //삭제

                                showListview();
                                len=true;
                            }
                        })
                        .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                len=true;
                                return;
                            }
                        });

                AlertDialog alertDialog = builder.create(); // 다이얼로그 보여주기 alertDialog.show();
                alertDialog.show();


                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //터치하면 전화 or 메세지 보내는 dialog창 뜨게
                if(len==true){

                    SOS item = (SOS) adapter.getItem(i);
                    selectname = item.getMname();
                    selectnumber = item.getMtel();
                    customDialog = new CustomDialog(SosActivity.this);
                    customDialog.show();
                    ///다이얼로그 안에있는 버튼들
                    callbtn = (ImageButton) customDialog.findViewById(R.id.call);
                    snsbtn = (ImageButton) customDialog.findViewById(R.id.sns);

                    callbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Toast.makeText(getApplicationContext(), selectnumber, Toast.LENGTH_SHORT).show();
                            //전화걸기 구현
                            Intent callIntent = new Intent(Intent.ACTION_CALL);
                            callIntent.setData(Uri.parse("tel:" + selectnumber));
                            startActivity(callIntent);
                        }
                    });

                    snsbtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //Toast.makeText(getApplicationContext(),selectname,Toast.LENGTH_SHORT).show();
                            //메세지 보내기 기능 구현

                            try {
                                //37.5407625,127.0793428
//                          List<Address> addresses = geoCoder.getFromLocation(lat,lon,1);
                                List<Address> addresses = geoCoder.getFromLocation(37.5407625, 127.0793428, 1);
                                //                         List<Address> addresses = geoCoder.getFromLocation(lat,lon,1);
                                // List<Address> addresses = geoCoder.getFromLocation(37.5407625,127.0793428,1);

                                StringBuilder sb = new StringBuilder();
                                Address address = addresses.get(0);
                                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                    sb.append(address.getAddressLine(i)).append("\n");
                                }

                                sb.append(address.getCountryName()).append(" ");//나라
                                sb.append(address.getAdminArea()).append(" ");
                                sb.append(address.getLocality()).append(" ");//시
                                sb.append(address.getThoroughfare()).append(" ");   //동
                                sb.append(address.getFeatureName()).append(" ");    // 번지

                                add = sb.toString();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }


                            //
                            String contents = "주소: " + add + "\n 제가 지금 위험해요!";
                            Messenger messenger = new Messenger(getApplicationContext());
                            messenger.sendMessageTo(selectnumber, contents);

                        }
                    });
                }
                //
            }
        });


        addbutton = (Button)findViewById(R.id.addnumber);

        addbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
                startActivityForResult(intent,0);
            }
        });

        showListview();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode == RESULT_OK){
            Cursor cursor = getContentResolver().query(data.getData(),new String[]{
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null);
            cursor.moveToFirst();
            name = cursor.getString(0);
            number = cursor.getString(1);
            //가져온거 디비에 저장
            final WarningHandler dbhandler = new WarningHandler(getApplicationContext());
            //만약 있는번호면 있다고 알림
            ///
            long check2 = dbhandler.check(name,number); //이 아이디가 댓글 달았는지 확인
            if (check2 == 1) {
                //이미등록
                Toast.makeText(this, "이미 등록된 전화번호입니다.", Toast.LENGTH_SHORT).show();
            } else {
                long check1 = dbhandler.insert(name,number);
                if(check1 == 0){
                    Toast.makeText(this, "오류", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(this, "등록완료!", Toast.LENGTH_SHORT).show();
                }
            }
            ///
            cursor.close();
            showListview();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    void showListview(){
        //리스트 갱신
        sos.clear();
        warningDB = new WarningDB(this);
        db = warningDB.getReadableDatabase();
        warningDB.onCreate(db);
        Cursor cursor2 = db.rawQuery("SELECT * FROM Warning",null);
        //int count = cursor.getCount();

        while(cursor2.moveToNext()){
            String nname = cursor2.getString(0); //name
            String nnumber = cursor2.getString(1);//number

            adapter.ADDSOS(new SOS(nname,nnumber));
        }
        cursor2.close();

        adapter.notifyDataSetChanged();

    }

    @Override
    public void onLocationChanged(Location location) {
         lat = location.getLatitude();
         lon = location.getLongitude();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    public class Messenger {
        private Context mContext;

        public Messenger(Context mContext){
            this.mContext = mContext;
        }

        public void sendMessageTo(String phoneNum,String message){
            SmsManager smsmanager = SmsManager.getDefault();
            smsmanager.sendTextMessage(phoneNum,null,message,null,null);
            Toast.makeText(mContext,"전송되었습니다",Toast.LENGTH_SHORT).show();
        }
    }



}
