package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BaseView<T> {

    /**
     * set connection between view and presenter
     */
    void setPresenter(T presenter);

}