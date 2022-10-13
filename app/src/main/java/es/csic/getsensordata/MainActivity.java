package es.csic.getsensordata;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

public class MainActivity extends Activity implements SensorEventListener, LocationListener, GpsStatus.Listener {
	// Flags:
	private Boolean Flag_Discover_Bluetooth=false;
	private Boolean flag_Trace=false;

	private SensorManager mSensorManager;
	private LocationManager locationManager;
	private TimerTask TaskReloj;
	private final Handler handlerReloj = new Handler();
	private Timer timerReloj;          // Timer

	//---
	GpsStatus gpsstatus=null;
	int num_satellites_in_view=0;
	int num_satellites_in_use=0;
	Sensor Sensor_Acc;
	Sensor Sensor_Gyr;
	Sensor Sensor_Mag;
	Sensor Sensor_AHRS;
	String texto_Acc_Features;
	String texto_Gyr_Features;
	String texto_Mag_Features;
	String texto_AHRS_Features;
	String texto_GNSS_Features;
	TextView obj_txtView0;
	TextView obj_txtView1;
	TextView obj_txtView1a;
	TextView obj_txtView1b;
	TextView obj_txtView2;
	TextView obj_txtView2a;
	TextView obj_txtView2b;
	TextView obj_txtView3;
	TextView obj_txtView3a;
	TextView obj_txtView3b;
	TextView obj_txtView9;
	TextView obj_txtView9a;
	TextView obj_txtView9b;
	TextView obj_txtView10;// GPS
	TextView obj_txtView10a;
	TextView obj_txtView10b;
	TextView obj_txtView10c;
	Button obj_btnBotonMarkPosition;
	ToggleButton obj_toggleButton1;
	ToggleButton obj_toggleButton2;
	ToggleButton obj_ToggleButtonSave;
	OutputStreamWriter fout;
	OutputStreamWriter fout_GNSS;
	OutputStreamWriter fout_AHRS;
	OutputStreamWriter fout_MAGN;
	OutputStreamWriter fout_GYRO;
	OutputStreamWriter fout_ACCE;
	// write to file after nomalization
	OutputStreamWriter fout_GNSS_nom;
	OutputStreamWriter fout_AHRS_nom;
	OutputStreamWriter fout_MAGN_nom;
	OutputStreamWriter fout_GYRO_nom;
	OutputStreamWriter fout_ACCE_nom;
	boolean primer_sensor_cambia=true;
	long tiempo_inicial_ns_raw=0;
	long timestamp_ns;
	double timestamp;
	long contador_Acce=0;
	long contador_Gyro=0;
	long contador_Magn=0;
	long contador_Ahrs=0;
	long contador_Gnss=0;
	long contador_Posi=0;
	float freq_medida_Acce=0;
	float freq_medida_Gyro=0;
	float freq_medida_Magn=0;
	float freq_medida_Ahrs=0;
	float freq_medida_Gnss=0;
	double timestamp_Acce_last=0;
	double timestamp_Gyro_last=0;
	double timestamp_Magn_last=0;
	double timestamp_Ahrs_last=0;
	double timestamp_Gnss_last=0;
	double timestamp_Acce_last_update=0;
	double timestamp_Gyro_last_update=0;
	double timestamp_Magn_last_update=0;
	double timestamp_Ahrs_last_update=0;
	double timestamp_Gnss_last_update=0;
	double deltaT_update=0.05;   // cada 0.25 segundo actualizo la pantalla con las medidas (4Hz)

	private BeaconManager beaconManager;
	private Region region;


	String phone_manufacturer;
	String phone_model;
	int phone_version;
	String phone_versionRelease;


	private static final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
	private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
	private File sub_path;

	private Timer timer;
	private volatile ArrayList<ArrayList<Float>> acce_data_sum = new ArrayList<ArrayList<Float>>(Arrays.asList(new ArrayList<Float>(),new ArrayList<Float>(),new ArrayList<Float>()));
	private int acce_data_count = 0; //防止timer执行时不同参数的Arraylist长度不同
	private volatile ArrayList<ArrayList<Float>> gyro_data_sum = new ArrayList<ArrayList<Float>>(Arrays.asList(new ArrayList<Float>(),new ArrayList<Float>(),new ArrayList<Float>()));
	private int gyro_data_count = 0;
	private volatile ArrayList<ArrayList<Float>> magn_data_sum = new ArrayList<ArrayList<Float>>(Arrays.asList(new ArrayList<Float>(),new ArrayList<Float>(),new ArrayList<Float>()));
	private int magn_data_count = 0;
	private volatile ArrayList<ArrayList<Float>> ahrs_data_sum = new ArrayList<ArrayList<Float>>(Arrays.asList(new ArrayList<Float>(),new ArrayList<Float>(),new ArrayList<Float>()));
	private int ahrs_data_count = 0;
	private volatile ArrayList<ArrayList<Float>> gnss_float_data_sum = new ArrayList<ArrayList<Float>>(Arrays.asList(new ArrayList<Float>(),new ArrayList<Float>(),new ArrayList<Float>()));
	private volatile ArrayList<ArrayList<Double>> gnss_double_data_sum = new ArrayList<ArrayList<Double>>(Arrays.asList(new ArrayList<Double>(),new ArrayList<Double>(),new ArrayList<Double>()));
	private int gnss_data_count = 0;
	private volatile boolean timer_flag=false;


	//======================OnCreate==================================
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
		}
		new GnssContainer(this).registerNmea();
		if (flag_Trace) {// start tracing to "/sdcard/GetSensorData_Trace.trace"
			Debug.startMethodTracing("GetSensorData_Trace");
		}

		phone_manufacturer = android.os.Build.MANUFACTURER;
		phone_model = android.os.Build.MODEL;
		phone_version = android.os.Build.VERSION.SDK_INT;
		phone_versionRelease = android.os.Build.VERSION.RELEASE;

		Log.i("OnCreate", "Phone_manufacturer " + phone_manufacturer
				+ " \n Phone_model " + phone_model
				+ " \n Phone_version " + phone_version
				+ " \n Phone_versionRelease " + phone_versionRelease
		);

		Log.i("OnCreate", "Inicializando");
		//------------Inicializar UI---------------
		Log.i("OnCreate", "Poner manejadores botones");
		inicializar_objetos_UI();
		poner_manejador_boton1();
		poner_manejador_boton2();
		poner_manejador_botonSave();
		poner_manejador_boton_MarkPosition();

		obj_txtView0.setText("Phone: " + phone_manufacturer+"  "+ phone_model+"  API"+phone_version+"  Android_"+phone_versionRelease);

		// ----------Ver los sensores internos disponibles ------------
		Log.i("OnCreate", "ver sensores internos disponibles");
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		Sensor_Acc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Sensor_Gyr = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		Sensor_Mag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		Sensor_AHRS = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

		// Mostrar datos generales del accelerometro:
		if (Sensor_Acc != null) {
			obj_txtView1.setText(" ACCE: "+Sensor_Acc.getName());
			texto_Acc_Features =" Manufacturer: "+Sensor_Acc.getVendor()
			+",\n Version: "+Sensor_Acc.getVersion()
			+", Type:"+Sensor_Acc.getType()
			+", \n Resolution: "+Sensor_Acc.getResolution()+" m/s^2"
			+", \n MaxRange: "+Sensor_Acc.getMaximumRange()+" m/s^2"
			+", \n Power consumption: "+Sensor_Acc.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Acc.getMinDelay();
		} else {
			obj_txtView1.setText(" ACCE: No Accelerometer detected");
			texto_Acc_Features =" No Features";
			obj_txtView1.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView1a.setText(texto_Acc_Features);

		// Mostrar Datos generales del gyroscope:
		if (Sensor_Gyr != null) {
			obj_txtView2.setText(" GYRO: "+Sensor_Gyr.getName());
			texto_Gyr_Features =" Manufacturer: "+Sensor_Gyr.getVendor()
			+",\n Version: "+Sensor_Gyr.getVersion()
			+", Type: "+Sensor_Gyr.getType()
			+", \n Resolution: "+Sensor_Gyr.getResolution()+" rad/s"
			+", \n MaxRange: "+Sensor_Gyr.getMaximumRange()+" rad/s"
			+", \n Power consumption: "+Sensor_Gyr.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Gyr.getMinDelay();
		} else {
			obj_txtView2.setText(" GYRO: No Gyroscope detected");
			texto_Gyr_Features =(String)" No Features";
			obj_txtView2.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView2a.setText(texto_Gyr_Features);

		// Mostrar Datos generales del Magnetometro:
		if (Sensor_Mag != null) {
			obj_txtView3.setText(" MAGN: "+Sensor_Mag.getName());
			texto_Mag_Features =" Manufacturer: "+Sensor_Mag.getVendor()
			+",\n Version: "+Sensor_Mag.getVersion()
			+", Type: "+Sensor_Mag.getType()
			+", \n Resolution: "+Sensor_Mag.getResolution()+" uT"
			+", \n MaxRange: "+Sensor_Mag.getMaximumRange()+" uT"
			+", \n Power consumption: "+Sensor_Mag.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_Mag.getMinDelay();
		} else {
			obj_txtView3.setText(" MAGN: No Magnetometer detected");
			texto_Mag_Features =(String)" No Features";
			obj_txtView3.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView3a.setText(texto_Mag_Features);

		// Mostrar Datos generales de Orientacion:
		if (Sensor_AHRS != null) {
			obj_txtView9.setText(" AHRS: "+Sensor_AHRS.getName());
			texto_AHRS_Features =" Manufacturer: "+Sensor_AHRS.getVendor()
			+",\n Version: "+Sensor_AHRS.getVersion()
			+", Type: "+Sensor_AHRS.getType()
			+", \n Resolution: "+Sensor_AHRS.getResolution()+" a.u."
			+", \n MaxRange: "+Sensor_AHRS.getMaximumRange()+" a.u."
			+", \n Power consumption: "+Sensor_AHRS.getPower()+" mA"
			+", \n MinDelay (0 means is not a streaming sensor): "+Sensor_AHRS.getMinDelay();
		} else {
			obj_txtView9.setText(" AHRS: No Attitude&Heading estimation");
			texto_AHRS_Features =(String)" No Features";
			obj_txtView9.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView9a.setText(texto_AHRS_Features);

		// ------------Ver los servicios de LOCALIZACION (GNSS/Network)--------------
		locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

		// Mostrar Datos generales de GNSS:
		if (locationManager != null) {
			LocationProvider provider = null;

			texto_GNSS_Features ="";
			obj_txtView10.setText(" GNSS: Location Service (GPS/Network)");

			//List<String> listaProviders=locationManager.getAllProviders();
			List<String> listaProviders=locationManager.getProviders(true);

			for (String provider_str : listaProviders) {
				int indice_proveedor=listaProviders.indexOf(provider_str);
				try {
					provider = locationManager.getProvider(provider_str);  // aqui CASCA con el emulador!!!!!!!!
				} catch (Exception e) {
					Log.i("OnCreate", "No responde bien getProvider");
				texto_GNSS_Features =" GNSS: No Location Providers";
				obj_txtView10.setBackgroundColor(0xFFFF0000);  // red color
				}
				if (provider != null) {
					texto_GNSS_Features =texto_GNSS_Features+ " -Location Provider"+indice_proveedor+": "+provider_str.toUpperCase()+
					", Accuracy: "+provider.getAccuracy()+", \n  Supports Altitude: "+provider.supportsAltitude()+
					", Power Cons.: "+provider.getPowerRequirement()+" mA"+"\n";
				}
			}
		} else {
			obj_txtView10.setText(" GNSS: No LOCATION system detected");
			texto_GNSS_Features =(String)"No Features";
			obj_txtView10.setBackgroundColor(0xFFFF0000);  // red color
		}
		obj_txtView10a.setText(texto_GNSS_Features);

        timer=new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timer_flag=true;
				double Timestamp=((double)(System.nanoTime()))*1E-9;
				float[] acce_res=nomalized(acce_data_sum,acce_data_count);
				float[] gyro_res=nomalized(gyro_data_sum,gyro_data_count);
				float[] magn_res=nomalized(magn_data_sum,magn_data_count);
				float[] ahrs_res=nomalized(ahrs_data_sum,ahrs_data_count);
				float[] gnss_float_res=nomalized(gnss_float_data_sum,gnss_data_count);
				double[] gnss_double_res=nomalized_double(gnss_double_data_sum,gnss_data_count);
				if (obj_ToggleButtonSave.isChecked())
				{
					try {
						String ACCE_file=String.format(Locale.US,"\nACCE;%.3f;%.5f;%.5f;%.5f",Timestamp,acce_res[0],acce_res[1],acce_res[2]);
						fout_ACCE_nom.write(ACCE_file);

						String GYRO_file=String.format(Locale.US,"\nGYRO;%.3f;%.5f;%.5f;%.5f",Timestamp,gyro_res[0],gyro_res[1],gyro_res[2]);
						fout_GYRO_nom.write(GYRO_file);

						String MAGN_file=String.format(Locale.US,"\nMAGN;%.3f;%.5f;%.5f;%.5f",Timestamp,magn_res[0],magn_res[1],magn_res[2]);
						fout_MAGN_nom.write(MAGN_file);

						String AHRS_file=String.format(Locale.US,"\nAHRS;%.3f;%.6f;%.6f;%.6f",Timestamp,ahrs_res[0],ahrs_res[1],ahrs_res[2]);
						fout_AHRS_nom.write(AHRS_file);

						String GNSS_file=String.format(Locale.US,"\nGNSS;%.3f;%.6f;%.6f;%.3f;%.3f;%.1f;%.1f;",Timestamp,gnss_double_res[0],gnss_double_res[1],gnss_double_res[2],gnss_float_res[0],gnss_float_res[1],gnss_float_res[2]);
						fout_GNSS_nom.write(GNSS_file);
					} catch (IOException e) {// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				arrayClear(acce_data_sum);
				arrayClear(gyro_data_sum);
				arrayClear(magn_data_sum);
				arrayClear(ahrs_data_sum);
				acce_data_count =0;
				gyro_data_count=0;
				magn_data_count=0;
				ahrs_data_count=0;
				timer_flag=false;
			}
		},1000,1000);

	} //---------------end OnCreate-----------


	// Nomalization calculation and write to file
	private float[] nomalized(ArrayList<ArrayList<Float>> arrayList,int data_count){
		float[] res=new float[arrayList.size()];
		int index=0;
		for(ArrayList<Float> s:arrayList) {
			float sum=0;
			for (int i=0;i<data_count;i++) {
				sum=sum+s.get(i);
			}
			res[index]=sum/data_count;
			index++;
		}
		return res;
	}

	private double[] nomalized_double(ArrayList<ArrayList<Double>> arrayList,int data_count){
		double[] res=new double[arrayList.size()];
		int index=0;
		for(ArrayList<Double> s:arrayList) {
			double sum=0;
			for (int i=0;i<data_count;i++) {
				sum=sum+s.get(i);
			}
			res[index]=sum/data_count;
			index++;
		}
		return res;
	}

	private void arrayClear(ArrayList<ArrayList<Float>> arrayList){
		for(ArrayList<Float> s:arrayList){
			s.clear();
		}
	}
	// M�todos
	private void inicializar_objetos_UI() {
		// Instanciar objetos locales del interfaz
		obj_txtView0 = (TextView)findViewById(R.id.textView0);

		obj_txtView1 = (TextView)findViewById(R.id.textView1);
		obj_txtView1.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView1a = (TextView)findViewById(R.id.textView1a);
		obj_txtView1b = (TextView)findViewById(R.id.textView1b);
		obj_txtView1a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView1a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView1b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView2 = (TextView)findViewById(R.id.textView2);
		obj_txtView2.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView2a = (TextView)findViewById(R.id.textView2a);
		obj_txtView2b = (TextView)findViewById(R.id.textView2b);
		obj_txtView2a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView2a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView2b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView3 = (TextView)findViewById(R.id.textView3);
		obj_txtView3.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView3a = (TextView)findViewById(R.id.textView3a);
		obj_txtView3b = (TextView)findViewById(R.id.textView3b);
		obj_txtView3a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView3a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView3b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView9 = (TextView)findViewById(R.id.textView9);
		obj_txtView9.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView9a = (TextView)findViewById(R.id.textView9a);
		obj_txtView9b = (TextView)findViewById(R.id.textView9b);
		obj_txtView9a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView9a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView9b.setVisibility(View.GONE);  // GONE 0x08

		obj_txtView10 = (TextView)findViewById(R.id.textView10);
		obj_txtView10.setBackgroundColor(0xFF00FF00);  // green color
		obj_txtView10a = (TextView)findViewById(R.id.textView10a);
		obj_txtView10b = (TextView)findViewById(R.id.textView10b);
		obj_txtView10c = (TextView)findViewById(R.id.textView10c);
		obj_txtView10a.setBackgroundColor(0xFFAFAFAF);  // gray color
		obj_txtView10a.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView10b.setVisibility(View.GONE);  // GONE 0x08
		obj_txtView10c.setVisibility(View.GONE);  // GONE 0x08

		// Gestion de botones
		obj_toggleButton1 = (ToggleButton)findViewById(R.id.togglebutton1);
		obj_toggleButton1.setChecked(false);
		obj_toggleButton2 = (ToggleButton)findViewById(R.id.togglebutton2);
		obj_toggleButton2.setChecked(false);
		obj_ToggleButtonSave = (ToggleButton)findViewById(R.id.togglebuttonsave);
		obj_ToggleButtonSave.setChecked(false);
		obj_btnBotonMarkPosition = (Button)findViewById(R.id.BtnBotonMarkPosition);
	}

	private void poner_manejador_boton_MarkPosition() {
		obj_btnBotonMarkPosition.setText("Mark First Position");

		obj_btnBotonMarkPosition.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {

				// Gestionar las pulsaciones del boton marcar
				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					Log.i("OnBotonMarkPosition", "Posicion marcada con botón mientras grabo fichero");
					contador_Posi=contador_Posi+1;  //incremento contador
					obj_btnBotonMarkPosition.setText("Mark Next Position #"+(contador_Posi+1));
				} else {
					Log.i("OnBotonMarkPosition", "Posicion no marcada pues no estoy grabando fichero");
					if (contador_Posi == 0) {
						obj_btnBotonMarkPosition.setText("Mark First Position");
					} else {
						obj_btnBotonMarkPosition.setText("Mark Next Position #" + (contador_Posi + 1));
					}
					Toast.makeText(getApplicationContext(), "Not marked. Start saving first", Toast.LENGTH_SHORT).show();
				}


				if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
				{
					// Poner TimeStamp de la App (seg�n le llega el dato)
					long timestamp_ns_raw = System.nanoTime(); // in nano seconds
					if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
					{
						timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
					} else {
						timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
					}
					timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

					// grabar en fichero
//					try {
//						// POSI;Timestamp(s);Counter;Latitude(degrees); Longitude(degrees);floor ID(0,1,2..4);Building ID(0,1,2..3)
//						String cadena_file=String.format(Locale.US,"\nPOSI;%.3f;%d;%.8f;%.8f;%d;%d",timestamp,contador_Posi,0.0,0.0,0,0);
//						fout.write(cadena_file);
//					} catch (IOException e) {// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
				}
			}
		});
	}

	private void poner_manejador_boton1() {
		//...................................................................
		// Manejador de ToggleButton1 (mostrar o no datos generales de sensores)
		obj_toggleButton1.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (obj_toggleButton1.isChecked()) {
					obj_txtView1a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView2a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView3a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView9a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10a.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					Log.i("","Boton pulsado: Show Sensor Features");
				} else {
					obj_txtView1a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView2a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView3a.setVisibility(View.GONE);  // GONE 0x0
					obj_txtView9a.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10a.setVisibility(View.GONE);  // GONE 0x08
					Log.i("","Boton pulsado: Hide Sensor Features");
				}
	}
		});
	}

	private void poner_manejador_boton2() {
		//...................................................................
		// Manejador de ToggleButton2 (mostrar o no datos en tiempo real)
		obj_toggleButton2.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (obj_toggleButton2.isChecked()) {
					obj_txtView1b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView2b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView3b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView9b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10b.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					obj_txtView10c.setVisibility(View.VISIBLE);  // VISIBLE 0x00
					Log.i("","Boton pulsado: Show Real-time Data");
				} else {
					obj_txtView1b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView2b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView3b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView9b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10b.setVisibility(View.GONE);  // GONE 0x08
					obj_txtView10c.setVisibility(View.GONE);  // GONE 0x08
					Log.i("","Boton pulsado: Hide Real-time Data");
				}
	}
		});
	}

	private void poner_manejador_botonSave() {
		if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
			finish();
		}
		//...................................................................
		// Manejador de ToggleButtonSave
		obj_ToggleButtonSave.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				// Probar si es posible realizar almacenamiento externo
				boolean mExternalStorageAvailable = false;
				boolean mExternalStorageWriteable = false;
				String state = Environment.getExternalStorageState();

				if (Environment.MEDIA_MOUNTED.equals(state)) {
					// We can read and write the media
					mExternalStorageAvailable = mExternalStorageWriteable = true;
				} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
					// We can only read the media
					mExternalStorageAvailable = true;
					mExternalStorageWriteable = false;
				} else {
					// Something else is wrong. It may be one of many other states, but all we need
					//  to know is we can neither read nor write
					mExternalStorageAvailable = mExternalStorageWriteable = false;
				}
				Log.i("OnCreate","ALMACENAMIENTO EXTERNO:"+mExternalStorageAvailable+mExternalStorageWriteable);

				// Intentar almacenar o cerrar
				if (obj_ToggleButtonSave.isChecked())  // Comenzar a grabar
				{
					Log.i("OnCheckedchanged","Botón Save pulsado!. Me pongo a grabar...");

					long CpuTimeStamp = System.nanoTime(); // in nano seconds
					if (primer_sensor_cambia && obj_ToggleButtonSave.isChecked()) {
						tiempo_inicial_ns_raw=CpuTimeStamp;  // en nano segundos
						Log.i("","Tiempo inicial: "+tiempo_inicial_ns_raw+" ms");
						timestamp_Acce_last_update=0;
						timestamp_Gyro_last_update=0;
						timestamp_Magn_last_update=0;
						timestamp_Ahrs_last_update=0;
						timestamp_Gnss_last_update=0;
					}

					SimpleDateFormat sf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",Locale.US);  // formato de la fecha
					Date fecha_actual = new Date();  // coger la fecha de hoy
					String str_fecha_actual = sf.format(fecha_actual);  // formatear fecha

					try {
						if (mExternalStorageAvailable) {
							File path = Environment.getExternalStoragePublicDirectory("LogFiles_GetSensorData");
							Log.i("OnCheckedChanged","Path donde guardo - ExternalStoragePublic: "+path);
							path.mkdirs();  // asegurarse que el directorio "./../LogFiles_GetSensorData" existe
							sub_path = new File(path.toString()+File.separator+"logfile_"+str_fecha_actual);
							sub_path.mkdirs();
//							File fichero = new File(path.getAbsolutePath(), "logfile_"+str_fecha_actual+".txt");
//							fout =	new OutputStreamWriter(	new FileOutputStream(fichero));
							fout_GNSS_nom=	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "GNSS_nom.txt")));
							fout_GNSS =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "GNSS.txt")));
							fout_GNSS_nom.write("\n% GNSS/GPS data:      \t'GNSS;AppTimestamp(s);Latit(deg);Long(deg);Altitude(m);Bearing(deg);Accuracy(m);Speed(m/s)'");
							fout_GNSS.write("\n% GNSS/GPS data:      \t'GNSS;AppTimestamp(s);SensorTimeStamp(s);Latit(deg);Long(deg);Altitude(m);Bearing(deg);Accuracy(m);Speed(m/s);SatInView;SatInUse'");

							fout_AHRS_nom =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "AHRS_nom.txt")));
							fout_AHRS_nom.write("\n% Orientation data:   \t'AHRS;AppTimestamp(s);PitchX(deg);RollY(deg);YawZ(deg)'");
							fout_AHRS =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "AHRS.txt")));
							fout_AHRS.write("\n% Orientation data:   \t'AHRS;AppTimestamp(s);SensorTimestamp(s);PitchX(deg);RollY(deg);YawZ(deg);Quat(2);Quat(3);Quat(4);Accuracy(int)'");

							fout_MAGN_nom =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "MAGN_nom.txt")));
							fout_MAGN_nom.write("\n% Magnetometer data:  \t'MAGN;AppTimestamp(s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT)'");
							fout_MAGN =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "MAGN.txt")));
							fout_MAGN.write("\n% Magnetometer data:  \t'MAGN;AppTimestamp(s);SensorTimestamp(s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Accuracy(integer)'");

							fout_GYRO_nom =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "GYRO_nom.txt")));
							fout_GYRO_nom.write("\n% Gyroscope data:     \t'GYRO;AppTimestamp(s);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s)'");
							fout_GYRO =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "GYRO.txt")));
							fout_GYRO.write("\n% Gyroscope data:     \t'GYRO;AppTimestamp(s);SensorTimestamp(s);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Accuracy(integer)'");

							fout_ACCE_nom =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "ACCE_nom.txt")));
							fout_ACCE_nom.write("\n% Accelerometer data: \t'ACCE;AppTimestamp(s);Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2)'");
							fout_ACCE =	new OutputStreamWriter(	new FileOutputStream(new File(sub_path.getAbsolutePath(), "ACCE.txt")));
							fout_ACCE.write("\n% Accelerometer data: \t'ACCE;AppTimestamp(s);SensorTimestamp(s);Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Accuracy(integer)'");

							Log.i("OncheckedChanged","Abierto fichero 'Externo' para escribir");
						} else {
//							fout=	new OutputStreamWriter(	openFileOutput("logfile_"+str_fecha_actual+".txt", Context.MODE_PRIVATE));
							Log.i("OncheckedChanged","Abierto fichero 'Interno' para escribir");
						}

						Toast.makeText(getApplicationContext(), "Saving sensor data", Toast.LENGTH_SHORT).show();

//						fout.write("% LogFile created by the 'GetSensorData' App for Android.");
//						fout.write("\n% Date of creation: "+fecha_actual.toString());
//						fout.write("\n% Developed by LOPSI research group at CAR-CSIC, Spain (http://www.car.upm-csic.es/lopsi)");
//						fout.write("\n% Version 2.1 January 2018");
//						fout.write("\n% The 'GetSensorData' program stores information from Smartphone/Tablet internal sensors (Accelerometers, Gyroscopes, Magnetometers, Pressure, Ambient Light, Orientation, Sound level, GPS/GNSS positio, WiFi RSS, Cellular/GSM/3G signal strength,...) and also from external devices (e.g. RFCode RFID reader, XSens IMU, LPMS-B IMU or MIMU22BT)");
//						fout.write("\n%\n% Phone used for this logfile:");
//						fout.write("\n% Manufacturer:            \t"+phone_manufacturer);
//						fout.write("\n% Model:                   \t"+phone_model);
//						fout.write("\n% API Android version:     \t"+phone_version);
//						fout.write("\n% Android version Release: \t"+phone_versionRelease);
//						fout.write("\n%\n% LogFile Data format:");
//						fout.write("\n% Accelerometer data: \t'ACCE;AppTimestamp(s);SensorTimestamp(s);Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Accuracy(integer)'");
//						fout.write("\n% Gyroscope data:     \t'GYRO;AppTimestamp(s);SensorTimestamp(s);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Accuracy(integer)'");
//						fout.write("\n% Magnetometer data:  \t'MAGN;AppTimestamp(s);SensorTimestamp(s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Accuracy(integer)'");
//						fout.write("\n% Pressure data:      \t'PRES;AppTimestamp(s);SensorTimestamp(s);Pres(mbar);Accuracy(integer)'");
//						fout.write("\n% Light data:         \t'LIGH;AppTimestamp(s);SensorTimestamp(s);Light(lux);Accuracy(integer)'");
//						fout.write("\n% Proximity data:     \t'PROX;AppTimestamp(s);SensorTimestamp(s);prox(?);Accuracy(integer)'");
//						fout.write("\n% Humidity data:      \t'HUMI;AppTimestamp(s);SensorTimestamp(s);humi(Percentage);Accuracy(integer)'");
//						fout.write("\n% Temperature data:   \t'TEMP;AppTimestamp(s);SensorTimestamp(s);temp(Celsius);Accuracy(integer)'");
//						fout.write("\n% Orientation data:   \t'AHRS;AppTimestamp(s);SensorTimestamp(s);PitchX(deg);RollY(deg);YawZ(deg);Quat(2);Quat(3);Quat(4);Accuracy(int)'");
//						fout.write("\n% GNSS/GPS data:      \t'GNSS;AppTimestamp(s);SensorTimeStamp(s);Latit(deg);Long(deg);Altitude(m);Bearing(deg);Accuracy(m);Speed(m/s);SatInView;SatInUse'");
//						fout.write("\n% WIFI data:          \t'WIFI;AppTimestamp(s);SensorTimeStamp(s);Name_SSID;MAC_BSSID;Frequency;RSS(dBm);'"); // Added frequency by jtorres
//						// fout.write("\n% WIFI data:          \t'WIFI;AppTimestamp(s);SensorTimeStamp(s);Name_SSID;MAC_BSSID;RSS(dBm);'");               original
//						fout.write("\n% Bluetooth data:     \t'BLUE;AppTimestamp(s);Name;MAC_Address;RSS(dBm);'");
//						fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);iBeacon;MAC;RSSI(dBm);Power;MajorID;MinorID;UUID'"); // Added power and UUID by jtorres
//						// fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);iBeacon;MAC;RSSI(dBm);MajorID;MinorID;'");               original
//						fout.write("\n% BLE 4.0 data:       \t'BLE4;AppTimestamp(s);Eddystone;MAC;RSSI(dBm);instanceID;OptionalTelemetry[voltaje;temperature;uptime;count]");
//						fout.write("\n% Sound data:         \t'SOUN;AppTimestamp(s);RMS;Pressure(Pa);SPL(dB);'");
//						fout.write("\n% RFID Reader data:   \t'RFID;AppTimestamp(s);ReaderNumber(int);TagID(int);RSS_A(dBm);RSS_B(dBm);'");
//						fout.write("\n% IMU XSens data:     \t'IMUX;AppTimestamp(s);SensorTimestamp(s);Counter;Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Roll(deg);Pitch(deg);Yaw(deg);Quat(1);Quat(2);Quat(3);Quat(4);Pressure(mbar);Temp(Celsius)'");
//						fout.write("\n% IMU LPMS-B data:    \t'IMUL;AppTimestamp(s);SensorTimestamp(s);Counter;Acc_X(m/s^2);Acc_Y(m/s^2);Acc_Z(m/s^2);Gyr_X(rad/s);Gyr_Y(rad/s);Gyr_Z(rad/s);Mag_X(uT);;Mag_Y(uT);Mag_Z(uT);Roll(deg);Pitch(deg);Yaw(deg);Quat(1);Quat(2);Quat(3);Quat(4);Pressure(mbar);Temp(Celsius)'");
//						fout.write("\n% IMU MIMU22BT data:  \t'IMUI;AppTimestamp(s);Packet_count;Step_Counter;delta_X(m);delta_Y(m);delta_Z(m);delta_theta(degrees);Covariance4x4[1:10]'");
//						fout.write("\n% POSI Reference:    	\t'POSI;Timestamp(s);Counter;Latitude(degrees); Longitude(degrees);floor ID(0,1,2..4);Building ID(0,1,2..3);'");
//						fout.write("\n% ");
//						fout.write("\n% Note that there are two timestamps: ");
//						fout.write("\n%  -'AppTimestamp' is set by the Android App as data is read. It is not representative of when data is actually captured by the sensor (but has a common time reference for all sensors)");
//						fout.write("\n%  -'SensorTimestamp' is set by the sensor itself (the delta_time=SensorTimestamp(k)-SensorTimestamp(k-1) between two consecutive samples is an accurate estimate of the sampling interval). This timestamp is better for integrating inertial data. \n");
					} catch (Exception ex) {
						Log.e("Ficheros", "Error al escribir fichero a memoria del dispositivo");
					}
					// Lanzar el Timer a 1Hz de Pintar los segundos trascurridos con timer
					poner_manejador_Reloj();
					timerReloj = new Timer("Hilo Timer Reloj");
					timerReloj.schedule(TaskReloj, 1000, 1000);  // llamar a Timer cada 1 segundo (con retardo inicial de 1s)
				} else  // Parar de grabar
				{
					// Parar el Timer a 1Hz de Pintar los segundos trascurridos con timer
					timerReloj.cancel();

					Log.i("Oncheckedchanged","Botón Save pulsado para parar de grabar!. Cierro el fichero");
					try {
						primer_sensor_cambia=true;  // resetear marca tiempo
						tiempo_inicial_ns_raw=0;
//						fout.close();
						fout_GNSS.close();
						fout_AHRS.close();
						fout_MAGN.close();
						fout_GYRO.close();
						fout_ACCE.close();
						fout_GNSS_nom.close();
						fout_AHRS_nom.close();
						fout_MAGN_nom.close();
						fout_GYRO_nom.close();
						fout_ACCE_nom.close();
						Toast.makeText(getApplicationContext(), "End of Saving", Toast.LENGTH_SHORT).show();

					} catch (Exception ex) {
						Log.e("Ficheros", "Error al intentar cerrar el fichero de memoria interna");
					}

				}
			}
		});
	}

	private void poner_manejador_Reloj() {

		// Time
		// Manejador del Timer para hacer pintar el reloj
		TaskReloj = new TimerTask() {
			public void run() {
				handlerReloj.post(new Runnable() {
					public void run() {
				// ................Do something at Timer rate.....................
				// Averiguar los segundos trascurridos desde inicio de grabaci�n
				long timestamp_ns_raw = System.nanoTime(); // in nano seconds
				if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
						{
							timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
						} else {
							timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
						}
				long segundos_trascurridos = (long)(((double)(timestamp_ns))*1E-9);  // de nano_s a segundos (segundos desde el inicio de la grabacion)
				// pintar los segundos en alg�n sitio d ela pantalla (p.ej. en el boton de grabar)
				if (obj_ToggleButtonSave.isChecked())  // Comenzar a grabar
				{
					obj_ToggleButtonSave.setText("Stop Saving.\n "+Long.toString(segundos_trascurridos)+" s");
				}
			}
		} );
	}
		};
	}


	//======================onAccuracyChanged==================================0
	@Override
	public final void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
	}

	//======================onSensorChanged==================================
	@Override
	public final void onSensorChanged(SensorEvent event) {
		long accuracy = event.accuracy;

		// TimeStamp del Sensor (a poner en el log_file)
		long   SensorTimestamp_ns_raw =  event.timestamp;      // in nano seconds
		double SensorTimestamp = ((double)(SensorTimestamp_ns_raw))*1E-9;  // sde nano_s a segundo

		// Poner TimeStamp de la App (seg�n le llega el dato)
		long timestamp_ns_raw = System.nanoTime(); // in nano seconds
		if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
		{
			timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
		} else {
			timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
		}
		timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

		//Log.i("","timestamp (ns): "+timestamp_ns);
		//Log.i("","timestamp (s): "+timestamp);

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			contador_Acce++;
			//double resto=Math.IEEEremainder(contador_Acce, 10);
			if (SensorTimestamp - timestamp_Acce_last > 0) {
				freq_medida_Acce = (float) (0.9 * freq_medida_Acce + 0.1 / (SensorTimestamp - timestamp_Acce_last));
			} else {
				Log.e("ACCE SENSOR","timestamp<time0stamp_Acce_last");
			}
			timestamp_Acce_last=SensorTimestamp;

			// Many sensors return 3 values, one for each axis.
			float[] Acc_data = event.values;

			//To get average data after Nomalization
			if (!timer_flag) {
				acce_data_sum.get(0).add(Acc_data[0]);
				acce_data_sum.get(1).add(Acc_data[1]);
				acce_data_sum.get(2).add(Acc_data[2]);
				acce_data_count++;
				Log.d("Acc_data_count ",String.valueOf(acce_data_count));
				Log.d("Acc_data_0 ",String.valueOf(acce_data_sum.get(0).size()));
				Log.d("Acc_data_1 ",String.valueOf(acce_data_sum.get(1).size()));
				Log.d("Acc_data_2 ",String.valueOf(acce_data_sum.get(2).size()));
			}

			// Do something with this sensor value.
			if (timestamp-timestamp_Acce_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tAcc(X): \t%10.5f \tm/s^2\n\tAcc(Y): \t%10.5f \tm/s^2\n\tAcc(Z): \t%10.5f \tm/s^2\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Acc_data[0],Acc_data[1],Acc_data[2],freq_medida_Acce);
				obj_txtView1b.setText(cadena_display);
				timestamp_Acce_last_update=timestamp;
			}

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nACCE;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Acc_data[0],Acc_data[1],Acc_data[2],accuracy);
					fout_ACCE.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			contador_Gyro++;

			// Many sensors return 3 values, one for each axis.
			float[] Gyr_data = event.values;

			if (!timer_flag) {
				gyro_data_sum.get(0).add(Gyr_data[0]);
				gyro_data_sum.get(1).add(Gyr_data[1]);
				gyro_data_sum.get(2).add(Gyr_data[2]);
				gyro_data_count++;
			}

			// Do something with this sensor value.
			if (SensorTimestamp - timestamp_Gyro_last > 0) {
				freq_medida_Gyro = (float) (0.99 * freq_medida_Gyro + 0.01 / (SensorTimestamp - timestamp_Gyro_last));
			}
			timestamp_Gyro_last=SensorTimestamp;


			if (timestamp-timestamp_Gyro_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tGyr(X): \t%10.5f \trad/s\n\tGyr(Y): \t%10.5f \trad/s\n\tGyr(Z): \t%10.5f \trad/s\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Gyr_data[0],Gyr_data[1],Gyr_data[2],freq_medida_Gyro);
				obj_txtView2b.setText(cadena_display);
				timestamp_Gyro_last_update=timestamp;
			}

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nGYRO;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Gyr_data[0],Gyr_data[1],Gyr_data[2],accuracy);
					fout_GYRO.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			contador_Magn++;
			if (SensorTimestamp - timestamp_Magn_last > 0) {
				freq_medida_Magn = (float) (0.9 * freq_medida_Magn + 0.1 / (SensorTimestamp - timestamp_Magn_last));
			}
			timestamp_Magn_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] Mag_data = event.values;

			if (!timer_flag) {
				magn_data_sum.get(0).add(Mag_data[0]);
				magn_data_sum.get(1).add(Mag_data[1]);
				magn_data_sum.get(2).add(Mag_data[2]);
				magn_data_count++;
			}

			// Do something with this sensor value.
			if (timestamp-timestamp_Magn_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tMag(X): \t%10.5f \tuT\n\tMag(Y): \t%10.5f \tuT\n\tMag(Z): \t%10.5f \tuT\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",Mag_data[0],Mag_data[1],Mag_data[2],freq_medida_Magn);
				obj_txtView3b.setText(cadena_display);
				timestamp_Magn_last_update=timestamp;
			}
			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					String cadena_file=String.format(Locale.US,"\nMAGN;%.3f;%.3f;%.5f;%.5f;%.5f;%d",timestamp,SensorTimestamp,Mag_data[0],Mag_data[1],Mag_data[2],accuracy);
					fout_MAGN.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if

		if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
			contador_Ahrs++;
			freq_medida_Ahrs=(float) (0.9*freq_medida_Ahrs	+ 0.1/(SensorTimestamp-timestamp_Ahrs_last));
			timestamp_Ahrs_last=SensorTimestamp;
			// Many sensors return 3 values, one for each axis.
			float[] AHRS_data = event.values;
			float[] Rot_b_g={1, 0, 0, 0, 1, 0, 0, 0, 1};

			try {
				SensorManager.getRotationMatrixFromVector(Rot_b_g, AHRS_data);
			} catch (IllegalArgumentException e) {
				if (AHRS_data.length > 3) {
					// Note 3 bug
					float[] newVector = new float[] {
							AHRS_data[0],
							AHRS_data[1],
							AHRS_data[2]
					};
					SensorManager.getRotationMatrixFromVector(Rot_b_g, newVector);
				}
			}
			//............................

			float[] orientacion={0,0,0};  // AzimutZ[0], PitchX[1] and RollY[2]
			orientacion=SensorManager.getOrientation(Rot_b_g, orientacion);  // En radianes pero lo da en WND (West/North/Down)
			orientacion[0] = -orientacion[0];
			orientacion[1] = -orientacion[1]; // Paso de WND a de nuevo ENU (que es donde quiero trabajar)
			double PI=3.14159265358979323846;
			float yaw_Z=(float) (orientacion[0]*180/PI);  // Yaw, ejeZ en grados
			float pitch_X=(float) (orientacion[1]*180/PI);  // Pitch, eje X
			float roll_Y=(float) (orientacion[2]*180/PI);  // Roll, eje Y

			if (!timer_flag) {
				ahrs_data_sum.get(0).add(pitch_X);
				ahrs_data_sum.get(1).add(roll_Y);
				ahrs_data_sum.get(2).add(yaw_Z);
				ahrs_data_count++;
			}

			//DecimalFormat
			if (timestamp-timestamp_Ahrs_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
			{
				String cadena_display=String.format(Locale.US,"\tPitch(X): \t%10.3f \t\tdegrees\n\tRoll(Y): \t%10.3f \t\tdegrees\n\tYaw(Z): \t%10.3f \tdegrees\n\t\t\t\t\t\t\t\tFreq: %5.0f Hz",pitch_X,roll_Y,yaw_Z,freq_medida_Ahrs);
				obj_txtView9b.setText(cadena_display);
				timestamp_Ahrs_last_update=timestamp;
			}

			// Do something with this sensor value.

			if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
			{
				try {
					//String cadena_file=String.format(Locale.US,"\nAHRS;%.3f;%.3f;%.4f;%.4f;%.4f;%.6f;%.6f;%.6f;%.6f;%d;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f;%.9f",
					//		timestamp,SensorTimestamp,pitch_X,roll_Y,yaw_Z,AHRS_data[0],AHRS_data[1],AHRS_data[2],AHRS_data[3],accuracy,Rot_b_g[0],Rot_b_g[1],Rot_b_g[2],Rot_b_g[3],Rot_b_g[4],Rot_b_g[5],Rot_b_g[6],Rot_b_g[7],Rot_b_g[8]);

					String cadena_file=String.format(Locale.US,"\nAHRS;%.3f;%.3f;%.6f;%.6f;%.6f;%.8f;%.8f;%.8f;%d",
							timestamp,SensorTimestamp,pitch_X,roll_Y,yaw_Z,AHRS_data[0],AHRS_data[1],AHRS_data[2],accuracy);
					fout_AHRS.write(cadena_file);
				} catch (IOException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end-if


	}


	//======================onLocationChanged==================================
	// Called when location has changed
	public void onLocationChanged(Location location) { //
		Log.i("LocationChanged","Gps data");
		double latitude=location.getLatitude();
		double longitude=location.getLongitude();
		double altitude=location.getAltitude();
		float bearing=location.getBearing();
		float accuracy=location.getAccuracy();
		float speed=location.getSpeed();
		double SensorTimeStamp=(double)(location.getTime())/1000;   // lo pongo en segundos (desde 1 Enero 1970)
		String provider=location.getProvider();

		contador_Gnss++;

		if (!timer_flag) {
			gnss_double_data_sum.get(0).add(latitude);
			gnss_double_data_sum.get(1).add(longitude);
			gnss_double_data_sum.get(2).add(altitude);
			gnss_float_data_sum.get(0).add(bearing);
			gnss_float_data_sum.get(1).add(accuracy);
			gnss_float_data_sum.get(2).add(speed);
			gnss_data_count++;
		}

		// Poner TimeStamp de la App (seg�n le llega el dato)
		long timestamp_ns_raw = System.nanoTime(); // in nano seconds
		if (timestamp_ns_raw>=tiempo_inicial_ns_raw)   // "tiempo_inicial_ns_raw" inicializado al dar al boton de grabar
		{
			timestamp_ns = timestamp_ns_raw - tiempo_inicial_ns_raw;
		} else {
			timestamp_ns = (timestamp_ns_raw - tiempo_inicial_ns_raw) + Long.MAX_VALUE;
		}
		timestamp = ((double)(timestamp_ns))*1E-9;  // de nano_s a segundos

		if (timestamp - timestamp_Gnss_last > 0) {
			freq_medida_Gnss = (float) (0.9 * freq_medida_Gnss + 0.1 / (timestamp - timestamp_Gnss_last));
		}
		timestamp_Gnss_last=timestamp;
		/*	String text = String.format(
				"Lat:\t %f\nLong:\t %f\nAlt:\t %f\nBearing:\t %f\nAccuracy:\t %f\nSpeed:\t %f\nTime:\t %f",
				location.getLatitude(), location.getLongitude(), location.getAltitude(),
				location.getBearing(),location.getAccuracy(),location.getSpeed(),location.getTime()); // Leer la posicion */

		if (timestamp-timestamp_Gnss_last_update>deltaT_update) // cada 0.5 segundos actualizo la pantalla
		{
			String cadena_display=String.format(Locale.US,"\tLatitude: \t%10.6f \tdegrees\n\tLongitude: \t%10.6f \tdegrees\n",latitude,longitude);
			String cadena_display_aux="";
			if (location.hasAltitude()) {
				cadena_display_aux = String.format(Locale.US, "\tAltitude: \t%6.1f \t m\n", altitude);
			} else {
				cadena_display_aux = "\tAltitude: \t\t? \tm\n";
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasAccuracy()) {
				cadena_display_aux = String.format(Locale.US, "\tAccuracy: \t%8.3f \tm\n", accuracy);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tAccuracy: \t\t? \tm\n", accuracy);
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasBearing()) {
				cadena_display_aux = String.format(Locale.US, "\tBearing: \t\t%8.3f \tdegrees\n", bearing);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tBearing: \t\t? \tdegrees\n", bearing);
			}
			cadena_display=cadena_display+cadena_display_aux;

			if (location.hasSpeed()) {
				cadena_display_aux = String.format(Locale.US, "\tSpeed: \t%8.3f \tm\n", speed);
			} else {
				cadena_display_aux = String.format(Locale.US, "\tSpeed: \t\t? \tm\n", speed);
			}
			cadena_display=cadena_display+cadena_display_aux;

			cadena_display = cadena_display + String.format(Locale.US, "\tTime: \t%8.3f \ts\n", SensorTimeStamp);
			;

			cadena_display=cadena_display+String.format(Locale.US,"\t(Provider: \t%s;  Freq: %5.0f Hz)\n",provider.toUpperCase(),freq_medida_Gnss);

			// Do something with this location value.
			TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
			obj_txtView.setText(cadena_display);
			timestamp_Gnss_last_update=timestamp;
		}
		if (obj_ToggleButtonSave.isChecked())  // Si grabando datos en log
		{
			try {
				String cadena_file=String.format(Locale.US,"\nGNSS;%.3f;%.3f;%.6f;%.6f;%.3f;%.3f;%.1f;%.1f;%d;%d",timestamp,SensorTimeStamp,latitude,longitude,altitude,bearing,accuracy,speed,num_satellites_in_view,num_satellites_in_use);
				fout_GNSS.write(cadena_file);
			} catch (IOException e) {// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	//======================onGpsStatusChanged==================================
	@SuppressLint("MissingPermission") // jtorres added to fix compilation problem
	                                   // TODO: check permission as shown below
	public void onGpsStatusChanged(int event) {
//		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//			// TODO: Consider calling
//			//    ActivityCompat#requestPermissions
//			// here to request the missing permissions, and then overriding
//			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//			//                                          int[] grantResults)
//			// to handle the case where the user grants the permission. See the documentation
//			// for ActivityCompat#requestPermissions for more details.
//			return;
//		}

		gpsstatus=locationManager.getGpsStatus(gpsstatus);

		if(gpsstatus != null) {
			Iterable<GpsSatellite>satellites = gpsstatus.getSatellites();
			Iterator<GpsSatellite>sat = satellites.iterator();
			int num_inview=0;
			int num_inuse=0;
			String strGpsStats="";
			while (sat.hasNext()) {
				GpsSatellite satellite = sat.next();
				strGpsStats=strGpsStats+ "\n\t- PRN:" + satellite.getPrn() + ", Used:" + satellite.usedInFix() + ", SNR:" +
				satellite.getSnr() + ", Az:" + satellite.getAzimuth() + "º,\n\t   Elev: " + satellite.getElevation()+
				"º, Alma: "+satellite.hasAlmanac()+ ", Ephem: "+satellite.hasEphemeris();
				num_inview++;
				if (satellite.usedInFix())
				{num_inuse=num_inuse+1;}
			}
//			String texto="\tSatellites in View: "+num_inview+", Satellites in Use: "+num_inuse+strGpsStats;
			TextView obj_txtView = (TextView)findViewById(R.id.textView10c);
			String texto=GnssContainer.getNmeaMsg();
			Log.d("NMEA",texto);
			obj_txtView.setText(texto);

			num_satellites_in_view=num_inview;
			num_satellites_in_use=num_inuse;
		}
	}



	// =================Methods required by LocationListener=================
	public void onProviderDisabled(String provider) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Disabled");
	}
	public void onProviderEnabled(String provider) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Enabled");
	}
	public void onStatusChanged(String provider, int status, Bundle extras) {
		TextView obj_txtView = (TextView)findViewById(R.id.textView10b);
		obj_txtView.setText("GNSS Provider Sattus: "+ status);
	}


	//======================onCreateOptionsMenue==================================
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Log.i("Menu","Opcion 1 Menu_Settings pulsada!");
			startActivity(new Intent(MainActivity.this,PantallaPreferencias.class));
			return true;
		case R.id.menu_exit:
			Log.i("Menu","Opcion 2 Exit pulsada!");
			//MainActivity.this.onPause();
			//MainActivity.this.onStop();
			//MainActivity.this.onDestroy();

			MainActivity.this.finish();
			MainActivity.this.onDestroy();
			return true;
		case R.id.menu_about:
			Log.i("Menu","Opcion 2 About pulsada!");
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	//======================onSaveInstanceState==================================
	// Called to save UI state changes at the
	// end of the active lifecycle.
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		// Save UI state changes to the savedInstanceState.
		// This bundle will be passed to onCreate and
		// onRestoreInstanceState if the process is
		// killed and restarted by the run time.
		super.onSaveInstanceState(savedInstanceState);
	}

	//======================onRestoreInstanceState==================================
	// Called after onCreate has finished, use to restore UI state
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// Restore UI state from the savedInstanceState.
		// This bundle has also been passed to onCreate.
		// Will only be called if the Activity has been
		// killed by the system since it was last visible.
	}

	//----------showToast----------------------
	private void showToast(String msg)
	{
		Toast error = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		error.show();
	}

	//======================onResume==================================
	@Override
	protected void onResume() {
		super.onResume();
		int delay;

//		SystemRequirementsChecker.checkWithDefaultDialogs(this);  //用于以对话框的形式像用户请求不能主动获取的权限

//		if (flag_BLE==true) {
//			beaconManager.connect(new BeaconManager.ServiceReadyCallback() {
//				@Override
//				public void onServiceReady() {
//					long scanPeriodMillis=200;  // 5Hz  (maximum scan rate according to SDK and experimental; with 10 Hz or 100ms the result is the same as 5Hz)
//					beaconManager.setForegroundScanPeriod(scanPeriodMillis,0);
//					beaconManager.startRanging(region);                         // Scan for iBeacon BLE tags
//					scanId_Eddystone = beaconManager.startEddystoneScanning();  // Scan for EddyStone BLE tags
//					if (flag_EstimoteTelemetry){
//					scanId = beaconManager.startTelemetryDiscovery();}
//				}
//			});
//
//
//
//		}


		Log.i("OnResume", "Estoy en OnResume");

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
		switch (Integer.parseInt(pref.getString("opcion1", "2"))) {
			case 1:
				delay = SensorManager.SENSOR_DELAY_FASTEST;
				Log.i("OnResume", "Opcion 1: SENSOR_DELAY_FASTEST");
				break;
			case 2:
				delay = SensorManager.SENSOR_DELAY_GAME;
				Log.i("OnResume", "Opcion 1: SENSOR_DELAY_GAME");
				break;
			case 3:
				delay = SensorManager.SENSOR_DELAY_NORMAL;
				Log.i("OnResume", "Opcion 1: SENSOR_DELAY_NORMAL");
				break;
			case 4:
				delay = SensorManager.SENSOR_DELAY_UI;
				Log.i("OnResume", "Opcion 1: SENSOR_DELAY_UI");
				break;
			default:
				delay = SensorManager.SENSOR_DELAY_GAME;
		}
		// Ver si quiero fijar la frecuencia a mi gusto en Hz
		Boolean agusto = pref.getBoolean("opcion5", false);
		if (agusto) {
			try {
				double freq = Integer.parseInt(pref.getString("opcion2", "100"));
				delay = (int) (1 / freq * 1000000);
			} catch (Exception x) {
				Log.i("Preference", "Update rate in Hz not parsed");
			}
		}


		//.....register sensors............
		if (Sensor_Acc != null) {
			mSensorManager.registerListener(this, Sensor_Acc, delay);
		}
		if (Sensor_Gyr != null) {
			mSensorManager.registerListener(this, Sensor_Gyr, delay);
		}
		if (Sensor_Mag != null) {
			mSensorManager.registerListener(this, Sensor_Mag, delay);
		}
		if (Sensor_AHRS != null) {
			mSensorManager.registerListener(this, Sensor_AHRS, delay);
		}
		Log.i("OnResume", "mSensorManager registered again");

		// ........Register Location manager...........
		if (locationManager != null) {
			if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//				ActivityCompat.requestPermissions( this, new String[] {  android.Manifest.permission.ACCESS_COARSE_LOCATION  },	MY_PERMISSION_ACCESS_COARSE_LOCATION );
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSION_ACCESS_FINE_LOCATION);
				finish();
			}
			locationManager.addGpsStatusListener(this);
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 200, 0, this);
				Log.i("OnResume", "GPS provider requested");
			}
			if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 200, 0, this);
				Log.i("OnResume", "NETWORK provider requested");
			}
			if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
				locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 200, 0, this);
				Log.i("OnResume", "PASSIVE provider requested");
			}
		}
		Log.i("OnResume", "locationManager registered again");
	}

	//======================onStart==================================
	@Override
	protected void onStart() {
		super.onStart();
		Log.i("OnStart","Estoy en OnStart");
	}

	//======================onReStart==================================
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.i("OnRestart","Estoy en OnRestart");
	}

	//======================onPause==================================
	@Override
	protected void onPause() {
		super.onPause();

		Log.i("OnPause", "INI: OnPause");

		//......unregister Sensors.................
		mSensorManager.unregisterListener(this);
		locationManager.removeUpdates(this);

	}
	//======================onStop==================================
	@Override
	protected void onStop() {
		super.onStop();
		Log.i("OnStop","Estoy en OnStop");
	}

	//======================onDestroy==================================
	@Override
	protected void onDestroy() {
		if (timer != null) {

			timer.cancel( );

			timer = null;

		}

		super.onDestroy();

		Log.i("OnDestroy","INI: OnDestroy");
		Log.i("OnDestroy","END: OnDestroy");
	}



}  // end - MainActivity
