package jp.techacademy.rei.nishimura.qa_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*
import kotlin.collections.HashMap


class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private lateinit var mFavoriteRef: DatabaseReference

    // お気に入りのフラグ
    var isFavorite = false

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(daraSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(snapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(error: DatabaseError) {

        }
    }

    // お気に入りのリスナー
    val mFavoriteListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
//            val map = dataSnapshot.value as Map<*, *>

            // お気に入り登録したquestionUidを取得
            val questionUid = dataSnapshot.value
            // お気に入り登録されていなかったらボーダーのアイコン
            if (questionUid == null) {
                isFavorite = false
                              favoriteButton.setImageResource(R.drawable.ic_baseline_favorite_border_24)
            // お気に入り登録されていたらボーダーじゃないアイコン
            } else {
                isFavorite = true
                favoriteButton.setImageResource(R.drawable.ic_baseline_favorite_24)
            }
        }

        override fun onCancelled(databaseError: DatabaseError) {
            // Getting Post failed, log a message
            Log.w("AA", "loadPost:onCancelled", databaseError.toException())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras!!.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            // ログインしていなければ非表示
            favoriteButton.visibility = View.INVISIBLE
        } else {
            // ログインしていたら表示
            favoriteButton.visibility = View.VISIBLE
        }


        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        setUp()

        val data = HashMap<String, Int>()

        data["genre"] = mQuestion.genre

        // お気に入りボタン押したとき
        favoriteButton.setOnClickListener {
            // お気に入りボタンが押されてるか判定
            if (isFavorite) {
                // お気に入り登録を削除
                favoriteButton.setImageResource(R.drawable.ic_baseline_favorite_border_24)
                mFavoriteRef.removeValue()
            } else {
                // お気に入り登録を追加
                favoriteButton.setImageResource(R.drawable.ic_baseline_favorite_24)
                mFavoriteRef.setValue(data)
            }
        }
    }

    fun setUp() {
        // データベースのリファレンス取得
        val databaseReference = FirebaseDatabase.getInstance().reference
        // contents/genre/questionUid/answers取得
        mAnswerRef = databaseReference.child(ContentsPATH).child(mQuestion.genre.toString())
            .child(mQuestion.questionUid).child(AnswersPATH)
        // answersの中が変更されたらmEventListenerが呼び出される（初回と変更されたとき）
        mAnswerRef.addChildEventListener(mEventListener)

        // ログインユーザー（自分）を判別する
        val mUid = FirebaseAuth.getInstance().currentUser?.uid
        //favorites/Uid/questionUid取得
        mFavoriteRef = databaseReference.child(FavoritesPATH).child(mUid.toString())
            .child(mQuestion.questionUid)
        // questionUidが変更されたらmFavoriteListenerが呼び出される（初回と変更されたとき）
        mFavoriteRef.addValueEventListener(mFavoriteListener)
    }

    override fun onRestart() {
        super.onRestart()
        // ログイン済みのユーザーを取得する
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            // ログインしていなければ非表示
            favoriteButton.visibility = View.INVISIBLE
        } else {
            // ログインしていたら表示
            // --- ここから ---
            favoriteButton.visibility = View.VISIBLE
            // --- ここまで ---
        }

        setUp()
    }

}
