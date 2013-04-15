package com.adp.threads;

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity {
  private TextView mDisplay;

  // TODO: set this to '1', '2', or '3'
  private int mExample = 2;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mDisplay = (TextView) findViewById(R.id.display);

    switch (mExample) {
      case 1: exampleOne(); break;
      case 2: exampleTwo(); break;
      case 3: exampleThree(); break;
      default: throw new RuntimeException();
    }

    // Display all threads to the screen
    printThreads();
  }

  /**
   * Print all background threads to the screen.
   */
  private void printThreads() {
    mDisplay.setText("");
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if (t.toString().startsWith("Thread #")) {
        mDisplay.append(t.toString() + "\n");
      }
    }
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
          SystemClock.sleep(250);
        }
      }

      @Override
      public String toString() {
        return "Thread #" + getId() + " (running...)";
      }
    }.start();
  }

  /***********************/
  /***** EXAMPLE TWO *****/
  /***********************/

  /**
   * This example avoids leaking an Activity context by declaring the thread as
   * a private static inner class, but the threads still continue to run even
   * across configuration changes. The JVM has a reference to all running
   * threads and does not whether or not these threads garbaged collected has
   * nothing to do with the Activity lifecycle. Threads which are left active
   * will continue to run until the kernel destroys your application's process.
   */
  private void exampleTwo() {
    new MyThread().start();
  }

  /**
   * Static inner classes don't hold implicit references to their outer class,
   * so the Activity instance won't be leaked across the configuration change.
   */
  private static class MyThread extends Thread {
    private boolean mRunning = false;

    @Override
    public void run() {
      mRunning = true;
      while (mRunning) {
        SystemClock.sleep(250);
      }
    }

    public void close() {
      mRunning = false;
    }

    @Override
    public String toString() {
      String status = mRunning ? "(running...)" : "(closing...)";
      return "Thread #" + getId() + " " + status;
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
    if (mExample == 3) {
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

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_kill:
        // Force kill the process (for your convenience :P)
        Process.killProcess(Process.myPid());
        return true;
    }
    return super.onOptionsItemSelected(item);
  }
}
