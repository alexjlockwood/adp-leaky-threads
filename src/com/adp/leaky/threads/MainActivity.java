package com.adp.leaky.threads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
  private static final String EXAMPLE_ID = "example_id";

  private RadioGroup mExampleGroup;
  private TextView mDisplay;
  private int mExampleId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mExampleId = R.id.example_one;
    if (savedInstanceState != null) {
      mExampleId = savedInstanceState.getInt(EXAMPLE_ID);
    }

    mDisplay = (TextView) findViewById(R.id.display);
    mExampleGroup = (RadioGroup) findViewById(R.id.example_group);
    ((RadioButton) findViewById(mExampleId)).setChecked(true);
    mExampleGroup.setOnCheckedChangeListener(this);

    runExample();
  }

  @Override
  protected void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);
    savedInstanceState.putInt(EXAMPLE_ID, mExampleId);
  }

  /***********************/
  /***** EXAMPLE ONE *****/
  /***********************/

  /**
   * Example illustrating how threads persist across configuration changes that
   * destroy the underlying Activity instance. The Activity context also leaks
   * because the thread is instantiated as an anonymous class, which holds an
   * implicit reference to the outer Activity instance, therefore preventing it
   * from being garbage collected.
   */
  private void exampleOne() {
    new Thread() {
      @Override
      public void run() {
        while (true) {
          try {
            Thread.sleep(250);
          } catch (InterruptedException e) {
            break;
          }
        }
      }

      @Override
      public String toString() {
        return "Background Thread #" + getId() + " (running...)";
      }
    }.start();
  }

  /***********************/
  /***** EXAMPLE TWO *****/
  /***********************/

  /**
   * This example avoids leaking an Activity context by declaring the thread as
   * a private static inner class, but the threads still continue to run even
   * across configuration changes. The DVM has a reference to all running
   * threads and whether or not these threads are garbaged collected has nothing
   * to do with the Activity lifecycle. Active threads will continue to run
   * until the kernel destroys your application's process.
   */
  private void exampleTwo() {
    new MyThread().start();
  }

  /**
   * Static inner classes don't hold implicit references to their outer class,
   * so the Activity instance won't be leaked across the configuration change.
   */
  private static class MyThread extends Thread {
    private boolean mRunning = true;

    @Override
    public void run() {
      while (mRunning) {
        try {
          Thread.sleep(250);
        } catch (InterruptedException e) {
          mRunning = false;
        }
      }
    }

    public void close() {
      mRunning = false;
    }

    @Override
    public String toString() {
      String status = mRunning ? "(running...)" : "(closing...)";
      return "Background Thread #" + getId() + " " + status;
    }
  }

  /*************************/
  /***** EXAMPLE THREE *****/
  /*************************/

  private MyThread mThread;

  /**
   * Same as example two, except for this time we have implemented a
   * cancellation policy for our thread, ensuring that it is never leaked!
   * onDestroy() is usually a good place to close your active threads before
   * exiting the Activity.
   */
  private void exampleThree() {
    mThread = new MyThread();
    mThread.start();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mExampleId == R.id.example_three) {
      mThread.close();
    }
  }

  /************************/
  /***** OPTIONS MENU *****/
  /************************/

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_trigger_config_change:
        // Simulate a configuration change
        recreate();
        return true;
      case R.id.menu_kill_process:
        // Force kill the process (for your convenience :P)
        Process.killProcess(Process.myPid());
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**************************/
  /***** HELPER METHODS *****/
  /**************************/

  private void runExample() {
    switch (mExampleId) {
      case R.id.example_one: exampleOne(); break;
      case R.id.example_two: exampleTwo(); break;
      case R.id.example_three: exampleThree(); break;
    }
    printThreads();
  }

  /**
   * Print all background threads to the screen.
   */
  private void printThreads() {
    mDisplay.setText("");
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if (t.toString().startsWith("Background Thread #") && !t.isInterrupted()) {
        mDisplay.append(t.toString() + "\n");
      }
    }
  }

  /**
   * Interupts all running example threads.
   */
  private void resetThreads() {
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if (t.toString().startsWith("Background Thread #")) {
        t.interrupt();
      }
    }
  }

  @Override
  public void onCheckedChanged(RadioGroup group, int checkedId) {
    if (mExampleId != checkedId) {
      mExampleId = checkedId;
      resetThreads();
      runExample();
    }
  }
}
