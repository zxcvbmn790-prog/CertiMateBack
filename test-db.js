const mysql = require('mysql2/promise');

async function run() {
  const connection = await mysql.createConnection({
    host: '3.39.237.132',
    user: 'certimate',
    password: 'CertiMate!2026App',
    database: 'CertiMate'
  });

  try {
    // Change user 4's old history (learn_id 1~5) to point to 산업기사 questions (e.g. 81~85)
    await connection.execute('UPDATE user_quiz_history SET learn_id = 81 WHERE user_id = 4 AND learn_id = 1');
    await connection.execute('UPDATE user_quiz_history SET learn_id = 82 WHERE user_id = 4 AND learn_id = 2');
    await connection.execute('UPDATE user_quiz_history SET learn_id = 83 WHERE user_id = 4 AND learn_id = 3');
    await connection.execute('UPDATE user_quiz_history SET learn_id = 84 WHERE user_id = 4 AND learn_id = 4');
    await connection.execute('UPDATE user_quiz_history SET learn_id = 85 WHERE user_id = 4 AND learn_id = 5');
    
    // Delete the '기사' log
    await connection.execute('DELETE FROM user_learn_log WHERE user_id = 4 AND cert_id = 2');
    
    // Update the '산업기사' log to reflect their 85% score
    await connection.execute('UPDATE user_learn_log SET correct_rate = 85.0, study_time_min = 150 WHERE user_id = 4 AND cert_id = 1');
    
    console.log('Fixed user 4 data to 산업기사');
  } catch (err) {
    console.error(err);
  } finally {
    await connection.end();
  }
}

run();
