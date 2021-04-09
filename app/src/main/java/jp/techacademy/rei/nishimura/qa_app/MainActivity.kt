package jp.techacademy.rei.nishimura.qa_app

import android.content.Intent // ←追加
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle // ←追加
import androidx.core.view.GravityCompat // ←追加
import com.google.android.material.navigation.NavigationView // ←追加
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth // ←追加
import com.google.firebase.database.*
// findById()を呼び出さずに該当Viewを取得するために必要となるインポート宣言
import kotlinx.android.synthetic.main.activity_main.* // ←追加
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlinx.android.synthetic.main.app_bar_main.*  // ←追加
import kotlinx.android.synthetic.main.app_bar_main.fab
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.content_main.listView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener { // ←修正

    private var mGenre = 0 // ←追加

    // --- ここから ---
    private lateinit var mDatabaseReference: DatabaseReference
    private lateinit var mQuestionArrayList: ArrayList<Question>
    private lateinit var mAdapter: QuestionsListAdapter

    private var mGenreRef: DatabaseReference? = null

    private var mFavoriteRef: DatabaseReference? = null


    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
            setList(dataSnapshot)
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            // 変更があったQuestionを探す
            for (question in mQuestionArrayList) {
                if (dataSnapshot.key.equals(question.questionUid)) {
                    // このアプリで変更がある可能性があるのは回答(Answer)のみ
                    question.answers.clear()
                    val answerMap = map["answers"] as Map<String, String>?
                    if (answerMap != null) {
                        for (key in answerMap.keys) {
                            val temp = answerMap[key] as Map<String, String>
                            val answerBody = temp["body"] ?: ""
                            val answerName = temp["name"] ?: ""
                            val answerUid = temp["uid"] ?: ""
                            val answer = Answer(answerBody, answerName, answerUid, key)
                            question.answers.add(answer)
                        }
                    }
                    mAdapter.notifyDataSetChanged()
                }
            }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {

        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

        }

        override fun onCancelled(error: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ~~ ここから
        // idがtoolbarがインポート宣言により取得されているので
        // id名でActionBarのサポートを依頼
        setSupportActionBar(toolbar)

        // fabにClickリスナーを登録
        // --- ここから ---
        fab.setOnClickListener { view ->
            // ジャンルを選択していない場合(mGenre == 0)はエラーを表示するだけ
            if (mGenre == 0) {
                Snackbar.make(view, getString(R.string.question_no_select_genre), Snackbar.LENGTH_LONG).show()
            } else {

            }

            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            // ログインしていなければログイン画面に遷移させる
            if (user == null) {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // ジャンルを渡して質問作成画面を起動する
                val intent = Intent(applicationContext, QuestionSendActivity::class.java)
                intent.putExtra("genre", mGenre)
                startActivity(intent)
            }
            // ~~ ここまで
        }
        // --- ここまで修正 ---

        // ナビゲーションドロワーの設定
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.app_name, R.string.app_name)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)



        // Firebase
        mDatabaseReference = FirebaseDatabase.getInstance().reference

        // ListViewの準備
        mAdapter = QuestionsListAdapter(this)
        mQuestionArrayList = ArrayList<Question>()
        mAdapter.notifyDataSetChanged()

        listView.setOnItemClickListener { parent, view, position, id ->
            // Questionのインスタンスを渡して質問詳細画面を起動する
            val intent = Intent(applicationContext, QuestionDetailActivity::class.java)
            intent.putExtra("question", mQuestionArrayList[position])
            startActivity(intent)
        }

        menuSetUp()
    }

    override fun onResume() {
        super.onResume()
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        // 1:趣味を既定の選択とする
        if (mGenre == 0) {
            onNavigationItemSelected(navigationView.menu.getItem(0))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_settings) {
            val intent = Intent(applicationContext, SettingActivity::class.java)
            startActivity(intent)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    // お気に入りのリスナー
    val mFavoriteListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            // --- ここから ---
            // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
            mQuestionArrayList.clear()
            mAdapter.setQuestionArrayList(mQuestionArrayList)
            listView.adapter = mAdapter

            val map = dataSnapshot.value as Map<*, *>?
            if (map != null) {
                for (data in map) {

                    val genreMap = data.value as Map<String, Long>
                    val genre = genreMap["genre"] ?: ""
                    mDatabaseReference.child(ContentsPATH).child(genre.toString()).child(data.key.toString())
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onCancelled(databaseError: DatabaseError) {
                            }
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                if (dataSnapshot.value == null) {
                                    return
                                }
                                setList(dataSnapshot)
                            }
                        })
                }
            }
        }
        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.w("AA", "loadPost:onCancelled", databaseError.toException())
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.nav_hobby) {
            toolbar.title = getString(R.string.menu_hobby_label)
            mGenre = 1
        } else if (id == R.id.nav_life) {
            toolbar.title = getString(R.string.menu_life_label)
            mGenre = 2
        } else if (id == R.id.nav_health) {
            toolbar.title = getString(R.string.menu_health_label)
            mGenre = 3
        } else if (id == R.id.nav_compter) {
            toolbar.title = getString(R.string.menu_compter_label)
            mGenre = 4
        } else if (id == R.id.nav_favorite) {
            toolbar.title = getString(R.string.menu_favorite_label)
            mGenre = 5
        }

        // 左にドロワーが閉じる動き
        drawer_layout.closeDrawer(GravityCompat.START)

        // --- ここから ---
        // 質問のリストをクリアしてから再度Adapterにセットし、AdapterをListViewにセットし直す
        mQuestionArrayList.clear()
        mAdapter.setQuestionArrayList(mQuestionArrayList)
        listView.adapter = mAdapter

        // 選択したジャンルにリスナーを登録する
        if (mGenreRef != null) {
            mGenreRef!!.removeEventListener(mEventListener)
        }

        // 選択したジャンルにリスナーを登録する
        if (mFavoriteRef != null) {
            mFavoriteRef!!.removeEventListener(mFavoriteListener)
        }

        if (id == R.id.nav_favorite) {
            // ログインユーザー（自分）を判別する
            val mUid = FirebaseAuth.getInstance().currentUser?.uid
            //favorites/Uid/questionUid取得
            mFavoriteRef = mDatabaseReference.child(FavoritesPATH).child(mUid.toString())
            mFavoriteRef!!.addValueEventListener(mFavoriteListener)
        } else {
            mGenreRef = mDatabaseReference.child(ContentsPATH).child(mGenre.toString())
            mGenreRef!!.addChildEventListener(mEventListener)
        }

        return true
        // --- ここまで追加する ---
    }
    // ~~ ここまで

    override fun onRestart() {
        super.onRestart()

        menuSetUp()
    }

    fun menuSetUp() {
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        // ドロワーの非表示
        nav_view.menu.findItem(R.id.nav_favorite).isVisible = user != null
    }

    fun setList(dataSnapshot: DataSnapshot) {
        val map = dataSnapshot.value as Map<String,String>
        val title = map["title"] ?: ""
        val body = map["body"] ?: ""
        val name = map["name"] ?: ""
        val uid = map["uid"]  ?: ""
        val imageString = map["image"] ?: ""
        val bytes =
            if (imageString.isNotEmpty()) {
                Base64.decode(imageString, Base64.DEFAULT)
            } else {
                byteArrayOf()
            }

        val answerArrayList = ArrayList<Answer>()
        val answerMap = map["answers"] as Map<String,String>?
        if (answerMap != null) {
            for (key in answerMap.keys) {
                val temp = answerMap[key] as Map<String,String>
                val answerBody = temp["body"] ?: ""
                val answerName = temp["name"] ?: ""
                val answerUid = temp["uid"] ?: ""
                val answer = Answer(answerBody, answerName, answerUid, key)
                answerArrayList.add(answer)
            }
        }

        val question = Question(title, body, name, uid, dataSnapshot.key ?: "",
            mGenre, bytes, answerArrayList)
        mQuestionArrayList.add(question)
        mAdapter.notifyDataSetChanged()
    }
}