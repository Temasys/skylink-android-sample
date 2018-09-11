package sg.com.temasys.skylink.sdk.sampleapp;

/**
 * Created by muoi.pham on 20/07/18.
 */

public interface BaseService<T> {

    /**
     * set connection between service and presenter
     */
    void setPresenter(T presenter);
}
