package seven.drawalive.nodebase;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.File;
import java.util.HashMap;

public class NodeBaseApp extends LinearLayout implements NodeMonitorEvent {
   public NodeBaseApp(Context context, HashMap<String, Object> env) {
      super(context);
      setOrientation(LinearLayout.VERTICAL);
      _env = env;
      _appdir = (File)env.get("appdir");

      collectAppInformation();
      prepareLayout();
      prepareEvents();
   }

   public void collectAppInformation() {
      try {
         // get all app entries
         // e.g. /sdcard/.nodebase/app1/{entry1.js,entry2.js,...}
         File[] fentries = _appdir.listFiles();
         String[] entries = new String[fentries.length];
         int count = 0;
         _readme = "(This is a NodeBase app)";
         for (int i = fentries.length - 1; i >= 0; i--) {
            File fentry = fentries[i];
            entries[i] = null;
            if (!fentry.isFile()) continue;
            String name = fentry.getName();
            if (name.endsWith(".js")) {
               entries[i] = name;
               count ++;
            } else if (name.toLowerCase().compareTo("readme") == 0) {
               _readme = Storage.read(fentry.getAbsolutePath());
            } else if (name.toLowerCase().compareTo("config") == 0) {
               _config = new NodeBaseAppConfigFile(Storage.read(fentry.getAbsolutePath()));
            }
         }

         _appentries = new String[count];
         for (int i = entries.length - 1; i >= 0; i--) {
            if (entries[i] == null) continue;
            count --;
            _appentries[count] = entries[i];
         }
      } catch (Exception e) {
         Log.w("UI:NodeBaseApp", "fail", e);
      }
   }

   public void prepareLayout() {
      Context context = getContext();
      LinearLayout frame = new LinearLayout(context);
      frame.setOrientation(LinearLayout.HORIZONTAL);

      /*ImageView image = new ImageView(context);
      LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(64, 64);
      params.setMargins(1, 1, 1, 1);
      image.setLayoutParams(params);
      image.setMaxHeight(64);
      image.setMaxWidth(64);
      image.setMinimumHeight(64);
      image.setMinimumWidth(64);
      try {
         File imgfile = new File(_appdir.getAbsolutePath().concat("/icon.png"));
         if (imgfile.exists()) {
            Bitmap bmp = BitmapFactory.decodeFile(imgfile.getAbsolutePath());
            image.setImageBitmap(bmp);
         } else {
            image.setBackgroundResource(R.drawable.default_icon);
         }
      } catch (Exception e) {
      }
      frame.addView(image);*/

      LayoutParams param;
      LinearLayout contents = new LinearLayout(context);
      contents.setOrientation(LinearLayout.VERTICAL);

      _btnTitle = new Button(context);
      _btnTitle.setText(String.format("  App : %s", getAppName()));
      _btnTitle.setGravity(Gravity.LEFT);
      _btnTitle.setAllCaps(false);
      _btnTitle.setLayoutParams(UserInterface.buttonLeftStyle);
      UserInterface.themeAppTitleButton(_btnTitle, false);
      contents.addView(_btnTitle);

      _panelDetails = new LinearLayout(context);
      _panelDetails.setOrientation(LinearLayout.VERTICAL);
      _panelDetails.setVisibility(View.GONE);
      TextView label;
      label = new TextView(context);
      label.setText(_readme);
      _readme = null; // release memory
      _panelDetails.addView(label);

      TableLayout tbl = new TableLayout(context);
      TableRow tbl_r_t = null;
      tbl_r_t = new TableRow(context);
      label = new TextView(context);
      label.setText("Entry");
      tbl_r_t.addView(label);
      label = new TextView(context);
      label.setText("Params");
      tbl_r_t.addView(label);
      tbl.addView(tbl_r_t);
      tbl_r_t = new TableRow(context);
      _listEntries = new Spinner(context);
      _listEntries.setAdapter(
            new ArrayAdapter<>(
                  context, android.R.layout.simple_spinner_dropdown_item, _appentries));
      tbl_r_t.addView(_listEntries);
      _txtParams = new EditText(context);
      tbl_r_t.addView(_txtParams);
      tbl.addView(tbl_r_t);
      tbl.setStretchAllColumns(true);
      _panelDetails.addView(tbl);


      LinearLayout subview = new LinearLayout(context);
      subview.setOrientation(LinearLayout.HORIZONTAL);
      _btnStart = new Button(context);
      _btnStart.setText("Start");
      subview.addView(_btnStart);
      _btnStop = new Button(context);
      _btnStop.setText("Stop");
      _btnStop.setEnabled(false);
      subview.addView(_btnStop);
      _btnOpen = new Button(context);
      _btnOpen.setText("Open");
      _btnOpen.setEnabled(false);
      subview.addView(_btnOpen);
      _btnShare = new Button(context);
      _btnShare.setText("Share");
      _btnShare.setEnabled(false);
      subview.addView(_btnShare);
      _panelDetails.addView(subview);

      param = new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
      );
      param.setMargins(0, 5, 0, 0);
      contents.setLayoutParams(param);
      contents.addView(_panelDetails);

      frame.addView(contents);
      addView(frame);
   }

   public void prepareEvents() {
      _btnTitle.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            if (_panelDetails.getVisibility() == View.GONE) {
               _panelDetails.setVisibility(View.VISIBLE);
            } else {
               _panelDetails.setVisibility(View.GONE);
            }
         }
      });

      _btnStart.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            String appname = getAppName();
            _btnStart.setEnabled(false);
            _btnStop.setEnabled(true);
            _btnOpen.setEnabled(true);
            _btnShare.setEnabled(true);
            new Thread(new Runnable() {
               @Override
               public void run() {
                  String appname = getAppName();
                  long timestamp = System.currentTimeMillis();
                  while (System.currentTimeMillis() - timestamp < 3000 /* 3s timeout */) {
                     if (NodeService.services.containsKey(appname)) {
                        NodeMonitor monitor = NodeService.services.get(appname);
                        if (monitor.isDead()) {
                           // not guarantee but give `after` get chance to run
                           // if want to guarantee, `synchronized` isDead
                           NodeBaseApp.this.after(monitor.getCommand(), null);
                        } else {
                           monitor.setEvent(NodeBaseApp.this);
                        }
                        break;
                     }
                  }
               }
            }).start();
            NodeService.touchService(
                  getContext(),
                  new String[]{
                        NodeService.AUTH_TOKEN,
                        "start", appname,
                        String.format(
                              "%s/node/node %s/%s %s",
                              _env.get("datadir").toString(),
                              _appdir.getAbsolutePath(),
                              String.valueOf(_listEntries.getSelectedItem()),
                              _txtParams.getText().toString()
                        )
                  });
         }
      });

      _btnStop.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            _btnStart.setEnabled(true);
            _btnStop.setEnabled(false);
            _btnOpen.setEnabled(false);
            _btnShare.setEnabled(false);
            NodeService.touchService(getContext(), new String[]{
                  NodeService.AUTH_TOKEN,
                  "stop", getAppName()
            });
         }
      });

      _btnOpen.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            String app_url = String.format(
                  generateAppUrlTemplate(),
                  Network.getWifiIpv4(getContext())
            );
            External.openBrowser(getContext(), app_url);
         }
      });

      _btnShare.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View view) {
            String name = generateAppTitle();
            String app_url = String.format(
                  generateAppUrlTemplate(),
                  Network.getWifiIpv4(getContext())
            );
            External.shareInformation(
                  getContext(), "Share", "NodeBase",
                  String.format("[%s] is running at %s", name, app_url), null
            );
         }
      });
   }

   private String generateAppUrlTemplate() {
      String protocol = null, port = null, index = null;
      if (_config != null) {
         port = _config.get(null, "port");
         protocol = _config.get(null, "protocol");
         index = _config.get(null, "index");
      }
      if (port == null) port = ""; else port = ":" + port;
      if (protocol == null) protocol = "http";
      if (index == null) index = "";
      return protocol + "://%s" + String.format("%s%s", port, index);
   }

   private String generateAppTitle() {
      String name = null;
      if (_config != null) {
         name = _config.get(null, "name");
      }
      if (name == null) name = "NodeBase Service";
      return name;
   }

   public String getAppName() {
      return _appdir.getName();
   }

   @Override
   public void before(String[] cmd) {
      UserInterface.run(new Runnable() {
         @Override
         public void run() {
            _btnStart.setEnabled(false);
            _btnStop.setEnabled(false);
            _btnOpen.setEnabled(false);
            _btnShare.setEnabled(false);
         }
      });
   }

   @Override
   public void started(String[] cmd, Process process) {
      UserInterface.run(new Runnable() {
         @Override
         public void run() {
            _btnStart.setEnabled(false);
            _btnStop.setEnabled(true);
            _btnOpen.setEnabled(true);
            _btnShare.setEnabled(true);
            UserInterface.themeAppTitleButton(_btnTitle, true);
         }
      });
   }

   @Override
   public void error(String[] cmd, Process process) {
   }

   @Override
   public void after(String[] cmd, Process process) {
      UserInterface.run(new Runnable() {
         @Override
         public void run() {
            _btnStart.setEnabled(true);
            _btnStop.setEnabled(false);
            _btnOpen.setEnabled(false);
            _btnShare.setEnabled(false);
            UserInterface.themeAppTitleButton(_btnTitle, false);
            Alarm.showToast(
                  NodeBaseApp.this.getContext(),
                  String.format("\"%s\" stopped", getAppName())
            );
         }
      });
   }

   private HashMap<String, Object> _env;
   private File _appdir;
   private String[] _appentries;
   private LinearLayout _panelDetails;
   private Button _btnTitle, _btnStart, _btnStop, _btnOpen, _btnShare;
   private Spinner _listEntries;
   private EditText _txtParams;
   private String _readme;
   private NodeBaseAppConfigFile _config;
}
