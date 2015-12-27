/**
 * Created by Y on 2015-11-21.
 */
var express = require('express');
var model_sns = require('../models/sns.js');
var model_senti = require('../models/sentiment.js');
var model_search = require('../models/searchlog.js');
var controller_senti = require('../controllers/sentiment.js');
var http = require('http');
var urlencode = require('urlencode');
var async = require('async');
var net = require('net');

var router = express.Router();
/* GET home page. */
router.get('/home', function (req, res, next) {
    res.render('home');
});

router.get('/view', function (req, res, next) {
    var keyword = req.query.keyword;
    if(keyword.length < 2) {
        res.status(400).send("키워드를 2글자 이상 입력해주세요");
    }
    if (keyword !== undefined) {
        res.render('view', {key: keyword});
    }
});

router.get('/test', function (req, res, next) {
    res.render('test');
});


router.get('/result/search', function (req, res, next) {
    var now = new Date();
    var condition = new Date(now.valueOf() - (3 * 60 * 60 * 1000));
    model_search.get_log(condition, function (err, docs) {
        if (err) {
            console.log(err);
        }
        res.send(docs);
    });
});

router.get('/result/chart', function (req, res, next) {
    /*
     req: keyword
     res: {
     [
     "keyword":
     "date":
     "positive":
     "negative":
     "neutral":
     ],
     ...
     }
     */
    var keyword = req.query.keyword;
    var today = new Date();
    async.parallel([
            function (callback) {
                var date = new Date(today.valueOf() - 6 * (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                var date = new Date(today.valueOf() - 5 * (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                var date = new Date(today.valueOf() - 4 * (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                var date = new Date(today.valueOf() - 3 * (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                var date = new Date(today.valueOf() - 2 * (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                var date = new Date(today.valueOf() - (24 * 60 * 60 * 1000));
                getSentimentResults(keyword, convertDateToString(date), function (result) {
                    callback(null, result);
                });
            },
            function (callback) {
                getSentimentResults(keyword, convertDateToString(today), function (result) {
                    callback(null, result);
                });
            }
        ],
        function (err, results) {

            //$scope.labels = ['2006', '2007', '2008', '2009', '2010', '2011', '2012'];
            //$scope.series = ['Series A', 'Series B', 'Series C'];
            //
            //$scope.data = [
            //    [65, 59, 80, 81, 56, 55, 40],
            //    [28, 48, 40, 19, 86, 27, 90],
            //    [34, 21, 60, 31, 25, 90, 32]
            //];
            var options = {
                names: [],
                fields: ["긍정", "부정", "중립"],
                dataset: [],
                max: 0
            }
            var count = 0;
            var num = 7;
            var max = 10;
            results.forEach(function (result) {
                //날짜
                count++;
                var dd = (result.date.getDate()).toString();
                var mm = (result.date.getMonth() + 1).toString(); //January is 0!
                if (dd < 10) {
                    dd = '0' + dd
                }
                if (mm < 10) {
                    mm = '0' + mm
                }
                var str = mm + '-' + dd;
                options.names.push(str);
                //값

                var datavalues = [];
                datavalues.push(result.positive);
                datavalues.push(result.negative);
                datavalues.push(result.neutral);
                var arrmax = Math.max.apply(Math, datavalues);
                if (max < arrmax) {
                    max = arrmax;
                }

                options.dataset.push(datavalues);
                options.max = max;
                if (count == num) {
                    res.json(options);
                }

            });
        });
});


//게시글에서 단어 수 세기
router.get('/result/wordCount', function (req, res, next) {
    /*
     req: keyword
     res: [
     {
     "word": 동사 단어
     "count": 단어 나온 횟수
     },
     ...
     ]
     */
    var keyword = req.query.keyword;
    var wordObject = new Object();
    var docNum = 0;
    var docCount = 0;

    //keyword가 포함된 doc array가져오기
    model_sns.get_sns_list_by_word(keyword, function (err, docs) {
        if (docs.length == 0 || docs == "[]" || docs == null || docs == undefined) {
            res.send("[]");
        }
        docNum = docs.length;
        //doc
        async.parallel([
            function (callback) {
                docs.forEach(function (doc) {
                    docCount++;
                    var vCount = 0;
                    var vNum = 0;
                    if (doc.Verb == "[]" || doc.Verb == null) {
                        if (docNum == docCount) {
                            callback(null, wordObject);
                        }
                    }
                    else {
                        vNum = doc.Verb.length;
                        doc.Verb.forEach(function (vWord) {
                            vCount++;
                            if (wordObject[vWord.word] == undefined) {
                                wordObject[vWord.word] = 1;
                            }
                            else {
                                wordObject[vWord.word] += 1;
                            }
                            if (vNum == vCount) {
                                if (docNum == docCount) {
                                    callback(null, wordObject);
                                }
                            }
                        });
                    }
                });
            }
        ], function (err, result) {
            var arr = [];
            if (Object.keys(result[0]).length == 0) {
                res.send(arr);
            }
            else {
                var count = 0;
                var num = Object.keys(result[0]).length;
                for (var prop in result[0]) {
                    if (wordObject.hasOwnProperty(prop)) {
                        model_senti.get_sentiment_by_word(prop, function (err, sentiData) {
                            count++;
                            if (!(sentiData[0] == "[]" || sentiData[0] == null || sentiData[0] == undefined)) {
                                if (sentiData[0].sentiment == "긍정" || sentiData[0].sentiment == "부정" ||
                                    (sentiData[0].sentiment == "중립" && sentiData[0].sentiment_score != "0%")) {
                                    var sentiment;
                                    var styleScope;
                                    var sentimentScore = (parseFloat((sentiData[0].sentiment_score).split("%")[0]) / 100).toFixed(2);
                                    if (sentiData[0].sentiment == "긍정"  && sentimentScore >= 0.2) {
                                        sentiment = "positive";
                                        styleScope = "background: rgba(130, 220, 248, " + sentimentScore + ");";
                                        arr.push({
                                            "word": sentiData[0].word,
                                            "count": wordObject[sentiData[0].word],
                                            "sentiment": sentiment,
                                            "sentiment_score": sentimentScore,
                                            "style": styleScope
                                        });
                                    }
                                    if (sentiData[0].sentiment == "중립" && sentimentScore >= 0.6) {
                                        sentiment = "neutral";
                                        styleScope = "background: rgba(198, 198, 198, " + sentimentScore + ");";
                                        arr.push({
                                            "word": sentiData[0].word,
                                            "count": wordObject[sentiData[0].word],
                                            "sentiment": sentiment,
                                            "sentiment_score": sentimentScore,
                                            "style": styleScope
                                        });
                                    }
                                    if (sentiData[0].sentiment == "부정" && sentimentScore >= 0.2) {
                                        sentiment = "negative";
                                        styleScope = "background: rgba(254, 146, 137, " + sentimentScore + ");";
                                        arr.push({
                                            "word": sentiData[0].word,
                                            "count": wordObject[sentiData[0].word],
                                            "sentiment": sentiment,
                                            "sentiment_score": sentimentScore,
                                            "style": styleScope
                                        });
                                    }

                                }

                                if (num == count) {
                                    arr.sort(function (a, b) {
                                        return b.count - a.count;
                                    });
                                    res.send(arr);
                                }
                            }
                        });
                    }
                }
            }
        });
    });

});


//name -> SNS db 전체 게시글
router.get('/result/sentimentBar', function (req, res, next) {
    /*
     req: keyword
     res: {
     "keyword":
     "totalCount": 감정이있는 글 갯수
     "positiveCount": 긍정 글 갯수
     "negativeCount": 부정 글 갯수
     "neutralCount": 중립 글 갯수
     "positive": 긍정 퍼센트
     "negative": 부정 퍼센트
     "neutral": 중립 퍼센트
     "text": {
     positiveText:[
     {
     "Id": screenName
     "Text": 내용
     "URL": 주소
     "sentiment": 감정
     },
     ...
     ],
     negativeText:[ 위와같음... ],
     neutralText:[ 위와같음... ]
     }
     */
    var keyword = req.query.keyword;
    async.waterfall([
        //keyword가 포함된 doc array 가져오기
        function (callback_first) {
            var docNum = 0;
            var docCount = 0;
            var docsJson = {
                "keyword": keyword,
                "totalCount": 0,
                "positiveCount": 0,
                "negativeCount": 0,
                "neutralCount": 0,
                "positive": 0,
                "negative": 0,
                "neutral": 0,
                "text": new Array()
                //"text": {

                //    "positiveText": new Array(),
                //    "negativeText": new Array(),
                //    "neutralText": new Array()
                //}
            };
            model_sns.get_sns_list_by_word(keyword, function (err, docs) {
                if (err) {
                    return;
                }
                if (docs == undefined || docs.length == 0) {
                    res.json(docsJson);
                }
                //글 하나마다 실행
                docNum = docs.length;
                docs.forEach(function (doc) {
                    async.parallel({
                        //동, 명사에 대해서 수행
                        word: function (callback_second) {
                            var textJson = {};
                            textJson.name = doc.Name;
                            textJson.scrName = doc.ScreenName;
                            textJson.Text = doc.Text;
                            textJson.URL = "https://twitter.com/" + doc.ScreenName + "/status/" + doc.StatusId;
                            textJson.sentiment = doc.sentiment;
                            textJson.date = convertStringToDateView(doc.Time);

                            if (doc.sentiment == "긍정" || doc.sentiment == "부정" || doc.sentiment == "중립") {
                                docsJson.text.push(textJson);
                            }
                            //if (doc.sentiment == "긍정") {
                            //    docsJson.text.positiveText.push(textJson);
                            //} else if (doc.sentiment == "부정") {
                            //    docsJson.text.negativeText.push(textJson);
                            //} else if (doc.sentiment == "중립") {
                            //    docsJson.text.neutralText.push(textJson);
                            //}
                            callback_second(null, doc.sentiment);
                        }
                    }, function (err, result) {
                        //sentiment 결정
                        docCount++;
                        if (result.word != "없음") {
                            docsJson.totalCount++;
                        }
                        if (result.word == "긍정") {
                            docsJson.positiveCount++;
                        } else if (result.word == "부정") {
                            docsJson.negativeCount++;
                        } else if (result.word == "중립") {
                            docsJson.neutralCount++;
                        }
                        if (docCount == docNum) {
                            callback_first(null, docsJson);
                        }
                    });
                });
            });
        }
    ], function (err, result) {
        result.positive = (result.positiveCount * 100 / (result.positiveCount + result.negativeCount + result.neutralCount)).toFixed(2);
        result.negative = (result.negativeCount * 100 / (result.positiveCount + result.negativeCount + result.neutralCount)).toFixed(2);
        result.neutral = (100 - result.positive - result.negative).toFixed(2);
        if (result.totalCount != 0 && result.keyword != 'undefined') {
            model_search.set_log_by_word(keyword, function (err, result) {
                if (err)
                    console.log(err);
            });
        }
        res.json(result);
    });
});


function convertDateToString(date) {
    var dd = date.getDate();
    var mm = date.getMonth() + 1; //January is 0!
    var yyyy = date.getFullYear();
    if (dd < 10) {
        dd = '0' + dd
    }
    if (mm < 10) {
        mm = '0' + mm
    }
    dd = dd.toString();
    mm = mm.toString();
    yyyy = yyyy.toString();
    var result = yyyy + mm + dd;

    return result;
}

function convertStringToDateView(str) {
    var mm = str.substr(4, 2);
    var dd = str.substr(6, 2);
    var hh = str.substr(8, 2);
    var min = str.substr(10, 2);
    var result = mm + '/' + dd + ' ' + hh + ':' + min;
    return result;
}

function convertStringToDate(str) {
    if (!/^(\d){8}$/.test(str)) return "invalid date";
    var yyyy = str.substr(0, 4);
    var mm = str.substr(4, 2) - 1;
    var dd = str.substr(6, 2);
    return new Date(yyyy, mm, dd);
}

var getSentimentResults = function (keyword, dateStr, callback) {
    var date = convertStringToDate(dateStr);
    var docNum = 0;
    var docCount = 0;
    var result = {
        keyword: keyword,
        date: date,
        positive: 0,
        negative: 0,
        neutral: 0
    };
    model_sns.get_sns_list_by_word_and_date(keyword, dateStr, function (err, docs) {
        if (err) {
            console.log(err);
        }
        if (docs == null || docs.length == 0) {
            callback(result);
        }
        else {
            docNum = docs.length;
            docs.forEach(function (doc) {
                docCount++;
                if (doc.sentiment == "긍정") {
                    result.positive++;
                } else if (doc.sentiment == "부정") {
                    result.negative++;
                } else if (doc.sentiment == "중립") {
                    result.neutral++;
                }
                if (docNum == docCount) {
                    callback(result);
                }
            });
        }
    });
}


router.post('/input/analysis', function (req, res, next) {
    var text = req.body.text;
    var apiInfo = {
        "index": 3,
        "count": 0
    };
    var client = new net.Socket();
    client.connect(7000, '127.0.0.1', function () {
        var str = text + "\n";
        client.write(str);
    });

    client.on('data', function (data) {

        console.log(data.toString());
        var jsonstr = JSON.parse(data.toString());
        setTimeout(function () {
            jsonstr.text = text;
            controller_senti.analysis(jsonstr, apiInfo, function (result) {
                console.log(result);
                res.send(result);
            });

            client.destroy(); // kill client after server's response
        }, 2 * 1000);


    });

    client.on('close', function () {
        console.log('Connection closed');
    });
});

module.exports = router;
