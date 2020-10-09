package sg.com.temasys.skylink.sdk.sampleapp.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import sg.com.temasys.skylink.sdk.sampleapp.R

/**
 * Created by muoi.pham on 20/07/18.
 */
class ScreenActivity : AppCompatActivity() {
    private val SCREEN_FRAGMENT_TAG = "SCREEN_FRAGMENT"

    // presenter instance
    private var screenPresenter: ScreenPresenter? = null

    // view instance
    private var screenFragment: ScreenFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)

        // create presenter
        screenPresenter = ScreenPresenter(this)

        // check previous state in case of screen rotation
        // if it is new state, then create view instance
        // otherwise reuse the view instance and keep states
        if (savedInstanceState == null) {
            screenFragment = ScreenFragment.newInstance()
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.contentFrameScreen, screenFragment!!, SCREEN_FRAGMENT_TAG)
                    .commit()
        } else {
            screenFragment = supportFragmentManager
                    .findFragmentByTag(SCREEN_FRAGMENT_TAG) as ScreenFragment?
        }

        //link between view and presenter
        if (screenFragment != null) screenPresenter!!.setView(screenFragment)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        //Save the fragment's instance when changing configuration
        supportFragmentManager.putFragment(outState, SCREEN_FRAGMENT_TAG, screenFragment!!)
    }
}