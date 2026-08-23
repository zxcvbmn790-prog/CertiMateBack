const mysql = require('mysql2/promise');

async function check() {
  const connection = await mysql.createConnection({
    host: '3.39.237.132',
    user: 'certimate',
    password: 'CertiMate!2026App',
    database: 'CertiMate'
  });

  try {
    const certs = [
      { name: '정보처리기능사', agency: '한국산업인력공단', diff: '기능사' },
      { name: '정보보안기사', agency: '한국인터넷진흥원', diff: '기사' },
      { name: '정보보안산업기사', agency: '한국인터넷진흥원', diff: '산업기사' },
      { name: '빅데이터분석기사', agency: '한국데이터산업진흥원', diff: '기사' },
      { name: '데이터분석준전문가 (ADsP)', agency: '한국데이터산업진흥원', diff: '준전문가' },
      { name: 'SQL 개발자 (SQLD)', agency: '한국데이터산업진흥원', diff: '개발자' },
      { name: '네트워크관리사 2급', agency: '한국정보통신자격협회', diff: '2급' },
      { name: '리눅스마스터 1급', agency: '한국정보통신진흥협회', diff: '1급' },
      { name: '리눅스마스터 2급', agency: '한국정보통신진흥협회', diff: '2급' }
    ];
    
    for (const c of certs) {
      await connection.execute(
        'INSERT INTO certification (cert_name, agency, difficulty, views) VALUES (?, ?, ?, 0)',
        [c.name, c.agency, c.diff]
      );
    }
    console.log('Inserted additional IT certifications.');
  } catch (err) {
    console.error(err);
  } finally {
    await connection.end();
  }
}

check();
