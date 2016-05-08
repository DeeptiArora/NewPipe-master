/**
 * Created by Deepti on 5/5/2016.
 */

import org.junit.Test;
import org.schabi.newpipe.DownloadDialog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.Assert.assertTrue;

public class DownloadDialogTest
{
    @Test
    public void test_createFileName__WithNoSpecialCharacters(){


        DownloadDialog downloadDialog = new DownloadDialog();


        Method method = null;
        try {
            method = DownloadDialog.class.getDeclaredMethod("createFileName", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        method.setAccessible(true);
        String title = "This is title";
        String filename = null;
        try {
            filename = (String) method.invoke(downloadDialog, title);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assertTrue(false);
        }


        assertTrue(filename.equals(title));

    }

    @Test
    public void test_createFileName__WithSpecialCharacters1(){


        DownloadDialog downloadDialog = new DownloadDialog();


        Method method = null;
        try {
            method = DownloadDialog.class.getDeclaredMethod("createFileName", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        method.setAccessible(true);
        String title = "This*is*title";
        String filename = null;
        try {
            filename = (String) method.invoke(downloadDialog, title);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        assertTrue(filename.equals("This is title"));

    }

    @Test
    public void test_createFileName__WithSpecialCharacters2(){


        DownloadDialog downloadDialog = new DownloadDialog();


        Method method = null;
        try {
            method = DownloadDialog.class.getDeclaredMethod("createFileName", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        method.setAccessible(true);
        String title = "This#is*title";
        String filename = null;
        try {
            filename = (String) method.invoke(downloadDialog, title);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        assertTrue(filename.equals("This is title"));

    }

    @Test
    public void test_createFileName__WithSpecialCharacters3(){


        DownloadDialog downloadDialog = new DownloadDialog();


        Method method = null;
        try {
            method = DownloadDialog.class.getDeclaredMethod("createFileName", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        method.setAccessible(true);
        String title = "This/is\\title";
        String filename = null;
        try {
            filename = (String) method.invoke(downloadDialog, title);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        assertTrue(filename.equals("This is title"));

    }

    @Test
    public void test_createFileName__WithSpecialCharacters4(){


        DownloadDialog downloadDialog = new DownloadDialog();


        Method method = null;
        try {
            method = DownloadDialog.class.getDeclaredMethod("createFileName", String.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            assertTrue(false);
        }
        method.setAccessible(true);
        String title = "This:is;title#";
        String filename = null;
        try {
            filename = (String) method.invoke(downloadDialog, title);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        assertTrue(filename.equals("This is title"));

    }

}
