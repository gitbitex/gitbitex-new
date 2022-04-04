;(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
    typeof define === 'function' && define.amd ? define(factory) :
    global.moment = factory()
}(this, (function () { 'use strict';

    var hookCallback;

    function hooks () {
        return hookCallback.apply(null, arguments);
    }

    // This is done to register the method called with moment()
    // without creating circular dependencies.
    function setHookCallback (callback) {
        hookCallback = callback;
    }

    function isArray(input) {
        return input instanceof Array || Object.prototype.toString.call(input) === '[object Array]';
    }

    function isObject(input) {
        // IE8 will treat undefined and null as object if it wasn't for
        // input != null
        return input != null && Object.prototype.toString.call(input) === '[object Object]';
    }

    function isObjectEmpty(obj) {
        if (Object.getOwnPropertyNames) {
            return (Object.getOwnPropertyNames(obj).length === 0);
        } else {
            var k;
            for (k in obj) {
                if (obj.hasOwnProperty(k)) {
                    return false;
                }
            }
            return true;
        }
    }

    function isUndefined(input) {
        return input === void 0;
    }

    function isNumber(input) {
        return typeof input === 'number' || Object.prototype.toString.call(input) === '[object Number]';
    }

    function isDate(input) {
        return input instanceof Date || Object.prototype.toString.call(input) === '[object Date]';
    }

    function map(arr, fn) {
        var res = [], i;
        for (i = 0; i < arr.length; ++i) {
            res.push(fn(arr[i], i));
        }
        return res;
    }

    function hasOwnProp(a, b) {
        return Object.prototype.hasOwnProperty.call(a, b);
    }

    function extend(a, b) {
        for (var i in b) {
            if (hasOwnProp(b, i)) {
                a[i] = b[i];
            }
        }

        if (hasOwnProp(b, 'toString')) {
            a.toString = b.toString;
        }

        if (hasOwnProp(b, 'valueOf')) {
            a.valueOf = b.valueOf;
        }

        return a;
    }

    function createUTC (input, format, locale, strict) {
        return createLocalOrUTC(input, format, locale, strict, true).utc();
    }

    function defaultParsingFlags() {
        // We need to deep clone this object.
        return {
            empty           : false,
            unusedTokens    : [],
            unusedInput     : [],
            overflow        : -2,
            charsLeftOver   : 0,
            nullInput       : false,
            invalidMonth    : null,
            invalidFormat   : false,
            userInvalidated : false,
            iso             : false,
            parsedDateParts : [],
            meridiem        : null,
            rfc2822         : false,
            weekdayMismatch : false
        };
    }

    function getParsingFlags(m) {
        if (m._pf == null) {
            m._pf = defaultParsingFlags();
        }
        return m._pf;
    }

    var some;
    if (Array.prototype.some) {
        some = Array.prototype.some;
    } else {
        some = function (fun) {
            var t = Object(this);
            var len = t.length >>> 0;

            for (var i = 0; i < len; i++) {
                if (i in t && fun.call(this, t[i], i, t)) {
                    return true;
                }
            }

            return false;
        };
    }

    function isValid(m) {
        if (m._isValid == null) {
            var flags = getParsingFlags(m);
            var parsedParts = some.call(flags.parsedDateParts, function (i) {
                return i != null;
            });
            var isNowValid = !isNaN(m._d.getTime()) &&
                flags.overflow < 0 &&
                !flags.empty &&
                !flags.invalidMonth &&
                !flags.invalidWeekday &&
                !flags.weekdayMismatch &&
                !flags.nullInput &&
                !flags.invalidFormat &&
                !flags.userInvalidated &&
                (!flags.meridiem || (flags.meridiem && parsedParts));

            if (m._strict) {
                isNowValid = isNowValid &&
                    flags.charsLeftOver === 0 &&
                    flags.unusedTokens.length === 0 &&
                    flags.bigHour === undefined;
            }

            if (Object.isFrozen == null || !Object.isFrozen(m)) {
                m._isValid = isNowValid;
            }
            else {
                return isNowValid;
            }
        }
        return m._isValid;
    }

    function createInvalid (flags) {
        var m = createUTC(NaN);
        if (flags != null) {
            extend(getParsingFlags(m), flags);
        }
        else {
            getParsingFlags(m).userInvalidated = true;
        }

        return m;
    }

    // Plugins that add properties should also add the key here (null value),
    // so we can properly clone ourselves.
    var momentProperties = hooks.momentProperties = [];

    function copyConfig(to, from) {
        var i, prop, val;

        if (!isUndefined(from._isAMomentObject)) {
            to._isAMomentObject = from._isAMomentObject;
        }
        if (!isUndefined(from._i)) {
            to._i = from._i;
        }
        if (!isUndefined(from._f)) {
            to._f = from._f;
        }
        if (!isUndefined(from._l)) {
            to._l = from._l;
        }
        if (!isUndefined(from._strict)) {
            to._strict = from._strict;
        }
        if (!isUndefined(from._tzm)) {
            to._tzm = from._tzm;
        }
        if (!isUndefined(from._isUTC)) {
            to._isUTC = from._isUTC;
        }
        if (!isUndefined(from._offset)) {
            to._offset = from._offset;
        }
        if (!isUndefined(from._pf)) {
            to._pf = getParsingFlags(from);
        }
        if (!isUndefined(from._locale)) {
            to._locale = from._locale;
        }

        if (momentProperties.length > 0) {
            for (i = 0; i < momentProperties.length; i++) {
                prop = momentProperties[i];
                val = from[prop];
                if (!isUndefined(val)) {
                    to[prop] = val;
                }
            }
        }

        return to;
    }

    var updateInProgress = false;

    // Moment prototype object
    function Moment(config) {
        copyConfig(this, config);
        this._d = new Date(config._d != null ? config._d.getTime() : NaN);
        if (!this.isValid()) {
            this._d = new Date(NaN);
        }
        // Prevent infinite loop in case updateOffset creates new moment
        // objects.
        if (updateInProgress === false) {
            updateInProgress = true;
            hooks.updateOffset(this);
            updateInProgress = false;
        }
    }

    function isMoment (obj) {
        return obj instanceof Moment || (obj != null && obj._isAMomentObject != null);
    }

    function absFloor (number) {
        if (number < 0) {
            // -0 -> 0
            return Math.ceil(number) || 0;
        } else {
            return Math.floor(number);
        }
    }

    function toInt(argumentForCoercion) {
        var coercedNumber = +argumentForCoercion,
            value = 0;

        if (coercedNumber !== 0 && isFinite(coercedNumber)) {
            value = absFloor(coercedNumber);
        }

        return value;
    }

    // compare two arrays, return the number of differences
    function compareArrays(array1, array2, dontConvert) {
        var len = Math.min(array1.length, array2.length),
            lengthDiff = Math.abs(array1.length - array2.length),
            diffs = 0,
            i;
        for (i = 0; i < len; i++) {
            if ((dontConvert && array1[i] !== array2[i]) ||
                (!dontConvert && toInt(array1[i]) !== toInt(array2[i]))) {
                diffs++;
            }
        }
        return diffs + lengthDiff;
    }

    function warn(msg) {
        if (hooks.suppressDeprecationWarnings === false &&
                (typeof console !==  'undefined') && console.warn) {
            console.warn('Deprecation warning: ' + msg);
        }
    }

    function deprecate(msg, fn) {
        var firstTime = true;

        return extend(function () {
            if (hooks.deprecationHandler != null) {
                hooks.deprecationHandler(null, msg);
            }
            if (firstTime) {
                var args = [];
                var arg;
                for (var i = 0; i < arguments.length; i++) {
                    arg = '';
                    if (typeof arguments[i] === 'object') {
                        arg += '\n[' + i + '] ';
                        for (var key in arguments[0]) {
                            arg += key + ': ' + arguments[0][key] + ', ';
                        }
                        arg = arg.slice(0, -2); // Remove trailing comma and space
                    } else {
                        arg = arguments[i];
                    }
                    args.push(arg);
                }
                warn(msg + '\nArguments: ' + Array.prototype.slice.call(args).join('') + '\n' + (new Error()).stack);
                firstTime = false;
            }
            return fn.apply(this, arguments);
        }, fn);
    }

    var deprecations = {};

    function deprecateSimple(name, msg) {
        if (hooks.deprecationHandler != null) {
            hooks.deprecationHandler(name, msg);
        }
        if (!deprecations[name]) {
            warn(msg);
            deprecations[name] = true;
        }
    }

    hooks.suppressDeprecationWarnings = false;
    hooks.deprecationHandler = null;

    function isFunction(input) {
        return input instanceof Function || Object.prototype.toString.call(input) === '[object Function]';
    }

    function set (config) {
        var prop, i;
        for (i in config) {
            prop = config[i];
            if (isFunction(prop)) {
                this[i] = prop;
            } else {
                this['_' + i] = prop;
            }
        }
        this._config = config;
        // Lenient ordinal parsing accepts just a number in addition to
        // number + (possibly) stuff coming from _dayOfMonthOrdinalParse.
        // TODO: Remove "ordinalParse" fallback in next major release.
        this._dayOfMonthOrdinalParseLenient = new RegExp(
            (this._dayOfMonthOrdinalParse.source || this._ordinalParse.source) +
                '|' + (/\d{1,2}/).source);
    }

    function mergeConfigs(parentConfig, childConfig) {
        var res = extend({}, parentConfig), prop;
        for (prop in childConfig) {
            if (hasOwnProp(childConfig, prop)) {
                if (isObject(parentConfig[prop]) && isObject(childConfig[prop])) {
                    res[prop] = {};
                    extend(res[prop], parentConfig[prop]);
                    extend(res[prop], childConfig[prop]);
                } else if (childConfig[prop] != null) {
                    res[prop] = childConfig[prop];
                } else {
                    delete res[prop];
                }
            }
        }
        for (prop in parentConfig) {
            if (hasOwnProp(parentConfig, prop) &&
                    !hasOwnProp(childConfig, prop) &&
                    isObject(parentConfig[prop])) {
                // make sure changes to properties don't modify parent config
                res[prop] = extend({}, res[prop]);
            }
        }
        return res;
    }

    function Locale(config) {
        if (config != null) {
            this.set(config);
        }
    }

    var keys;

    if (Object.keys) {
        keys = Object.keys;
    } else {
        keys = function (obj) {
            var i, res = [];
            for (i in obj) {
                if (hasOwnProp(obj, i)) {
                    res.push(i);
                }
            }
            return res;
        };
    }

    var defaultCalendar = {
        sameDay : '[Today at] LT',
        nextDay : '[Tomorrow at] LT',
        nextWeek : 'dddd [at] LT',
        lastDay : '[Yesterday at] LT',
        lastWeek : '[Last] dddd [at] LT',
        sameElse : 'L'
    };

    function calendar (key, mom, now) {
        var output = this._calendar[key] || this._calendar['sameElse'];
        return isFunction(output) ? output.call(mom, now) : output;
    }

    var defaultLongDateFormat = {
        LTS  : 'h:mm:ss A',
        LT   : 'h:mm A',
        L    : 'MM/DD/YYYY',
        LL   : 'MMMM D, YYYY',
        LLL  : 'MMMM D, YYYY h:mm A',
        LLLL : 'dddd, MMMM D, YYYY h:mm A'
    };

    function longDateFormat (key) {
        var format = this._longDateFormat[key],
            formatUpper = this._longDateFormat[key.toUpperCase()];

        if (format || !formatUpper) {
            return format;
        }

        this._longDateFormat[key] = formatUpper.replace(/MMMM|MM|DD|dddd/g, function (val) {
            return val.slice(1);
        });

        return this._longDateFormat[key];
    }

    var defaultInvalidDate = 'Invalid date';

    function invalidDate () {
        return this._invalidDate;
    }

    var defaultOrdinal = '%d';
    var defaultDayOfMonthOrdinalParse = /\d{1,2}/;

    function ordinal (number) {
        return this._ordinal.replace('%d', number);
    }

    var defaultRelativeTime = {
        future : 'in %s',
        past   : '%s ago',
        s  : 'a few seconds',
        ss : '%d seconds',
        m  : 'a minute',
        mm : '%d minutes',
        h  : 'an hour',
        hh : '%d hours',
        d  : 'a day',
        dd : '%d days',
        M  : 'a month',
        MM : '%d months',
        y  : 'a year',
        yy : '%d years'
    };

    function relativeTime (number, withoutSuffix, string, isFuture) {
        var output = this._relativeTime[string];
        return (isFunction(output)) ?
            output(number, withoutSuffix, string, isFuture) :
            output.replace(/%d/i, number);
    }

    function pastFuture (diff, output) {
        var format = this._relativeTime[diff > 0 ? 'future' : 'past'];
        return isFunction(format) ? format(output) : format.replace(/%s/i, output);
    }

    var aliases = {};

    function addUnitAlias (unit, shorthand) {
        var lowerCase = unit.toLowerCase();
        aliases[lowerCase] = aliases[lowerCase + 's'] = aliases[shorthand] = unit;
    }

    function normalizeUnits(units) {
        return typeof units === 'string' ? aliases[units] || aliases[units.toLowerCase()] : undefined;
    }

    function normalizeObjectUnits(inputObject) {
        var normalizedInput = {},
            normalizedProp,
            prop;

        for (prop in inputObject) {
            if (hasOwnProp(inputObject, prop)) {
                normalizedProp = normalizeUnits(prop);
                if (normalizedProp) {
                    normalizedInput[normalizedProp] = inputObject[prop];
                }
            }
        }

        return normalizedInput;
    }

    var priorities = {};

    function addUnitPriority(unit, priority) {
        priorities[unit] = priority;
    }

    function getPrioritizedUnits(unitsObj) {
        var units = [];
        for (var u in unitsObj) {
            units.push({unit: u, priority: priorities[u]});
        }
        units.sort(function (a, b) {
            return a.priority - b.priority;
        });
        return units;
    }

    function zeroFill(number, targetLength, forceSign) {
        var absNumber = '' + Math.abs(number),
            zerosToFill = targetLength - absNumber.length,
            sign = number >= 0;
        return (sign ? (forceSign ? '+' : '') : '-') +
            Math.pow(10, Math.max(0, zerosToFill)).toString().substr(1) + absNumber;
    }

    var formattingTokens = /(\[[^\[]*\])|(\\)?([Hh]mm(ss)?|Mo|MM?M?M?|Do|DDDo|DD?D?D?|ddd?d?|do?|w[o|w]?|W[o|W]?|Qo?|YYYYYY|YYYYY|YYYY|YY|gg(ggg?)?|GG(GGG?)?|e|E|a|A|hh?|HH?|kk?|mm?|ss?|S{1,9}|x|X|zz?|ZZ?|.)/g;

    var localFormattingTokens = /(\[[^\[]*\])|(\\)?(LTS|LT|LL?L?L?|l{1,4})/g;

    var formatFunctions = {};

    var formatTokenFunctions = {};

    // token:    'M'
    // padded:   ['MM', 2]
    // ordinal:  'Mo'
    // callback: function () { this.month() + 1 }
    function addFormatToken (token, padded, ordinal, callback) {
        var func = callback;
        if (typeof callback === 'string') {
            func = function () {
                return this[callback]();
            };
        }
        if (token) {
            formatTokenFunctions[token] = func;
        }
        if (padded) {
            formatTokenFunctions[padded[0]] = function () {
                return zeroFill(func.apply(this, arguments), padded[1], padded[2]);
            };
        }
        if (ordinal) {
            formatTokenFunctions[ordinal] = function () {
                return this.localeData().ordinal(func.apply(this, arguments), token);
            };
        }
    }

    function removeFormattingTokens(input) {
        if (input.match(/\[[\s\S]/)) {
            return input.replace(/^\[|\]$/g, '');
        }
        return input.replace(/\\/g, '');
    }

    function makeFormatFunction(format) {
        var array = format.match(formattingTokens), i, length;

        for (i = 0, length = array.length; i < length; i++) {
            if (formatTokenFunctions[array[i]]) {
                array[i] = formatTokenFunctions[array[i]];
            } else {
                array[i] = removeFormattingTokens(array[i]);
            }
        }

        return function (mom) {
            var output = '', i;
            for (i = 0; i < length; i++) {
                output += isFunction(array[i]) ? array[i].call(mom, format) : array[i];
            }
            return output;
        };
    }

    // format date using native date object
    function formatMoment(m, format) {
        if (!m.isValid()) {
            return m.localeData().invalidDate();
        }

        format = expandFormat(format, m.localeData());
        formatFunctions[format] = formatFunctions[format] || makeFormatFunction(format);

        return formatFunctions[format](m);
    }

    function expandFormat(format, locale) {
        var i = 5;

        function replaceLongDateFormatTokens(input) {
            return locale.longDateFormat(input) || input;
        }

        localFormattingTokens.lastIndex = 0;
        while (i >= 0 && localFormattingTokens.test(format)) {
            format = format.replace(localFormattingTokens, replaceLongDateFormatTokens);
            localFormattingTokens.lastIndex = 0;
            i -= 1;
        }

        return format;
    }

    var match1         = /\d/;            //       0 - 9
    var match2         = /\d\d/;          //      00 - 99
    var match3         = /\d{3}/;         //     000 - 999
    var match4         = /\d{4}/;         //    0000 - 9999
    var match6         = /[+-]?\d{6}/;    // -999999 - 999999
    var match1to2      = /\d\d?/;         //       0 - 99
    var match3to4      = /\d\d\d\d?/;     //     999 - 9999
    var match5to6      = /\d\d\d\d\d\d?/; //   99999 - 999999
    var match1to3      = /\d{1,3}/;       //       0 - 999
    var match1to4      = /\d{1,4}/;       //       0 - 9999
    var match1to6      = /[+-]?\d{1,6}/;  // -999999 - 999999

    var matchUnsigned  = /\d+/;           //       0 - inf
    var matchSigned    = /[+-]?\d+/;      //    -inf - inf

    var matchOffset    = /Z|[+-]\d\d:?\d\d/gi; // +00:00 -00:00 +0000 -0000 or Z
    var matchShortOffset = /Z|[+-]\d\d(?::?\d\d)?/gi; // +00 -00 +00:00 -00:00 +0000 -0000 or Z

    var matchTimestamp = /[+-]?\d+(\.\d{1,3})?/; // 123456789 123456789.123

    // any word (or two) characters or numbers including two/three word month in arabic.
    // includes scottish gaelic two word and hyphenated months
    var matchWord = /[0-9]{0,256}['a-z\u00A0-\u05FF\u0700-\uD7FF\uF900-\uFDCF\uFDF0-\uFF07\uFF10-\uFFEF]{1,256}|[\u0600-\u06FF\/]{1,256}(\s*?[\u0600-\u06FF]{1,256}){1,2}/i;

    var regexes = {};

    function addRegexToken (token, regex, strictRegex) {
        regexes[token] = isFunction(regex) ? regex : function (isStrict, localeData) {
            return (isStrict && strictRegex) ? strictRegex : regex;
        };
    }

    function getParseRegexForToken (token, config) {
        if (!hasOwnProp(regexes, token)) {
            return new RegExp(unescapeFormat(token));
        }

        return regexes[token](config._strict, config._locale);
    }

    // Code from http://stackoverflow.com/questions/3561493/is-there-a-regexp-escape-function-in-javascript
    function unescapeFormat(s) {
        return regexEscape(s.replace('\\', '').replace(/\\(\[)|\\(\])|\[([^\]\[]*)\]|\\(.)/g, function (matched, p1, p2, p3, p4) {
            return p1 || p2 || p3 || p4;
        }));
    }

    function regexEscape(s) {
        return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
    }

    var tokens = {};

    function addParseToken (token, callback) {
        var i, func = callback;
        if (typeof token === 'string') {
            token = [token];
        }
        if (isNumber(callback)) {
            func = function (input, array) {
                array[callback] = toInt(input);
            };
        }
        for (i = 0; i < token.length; i++) {
            tokens[token[i]] = func;
        }
    }

    function addWeekParseToken (token, callback) {
        addParseToken(token, function (input, array, config, token) {
            config._w = config._w || {};
            callback(input, config._w, config, token);
        });
    }

    function addTimeToArrayFromToken(token, input, config) {
        if (input != null && hasOwnProp(tokens, token)) {
            tokens[token](input, config._a, config, token);
        }
    }

    var YEAR = 0;
    var MONTH = 1;
    var DATE = 2;
    var HOUR = 3;
    var MINUTE = 4;
    var SECOND = 5;
    var MILLISECOND = 6;
    var WEEK = 7;
    var WEEKDAY = 8;

    // FORMATTING

    addFormatToken('Y', 0, 0, function () {
        var y = this.year();
        return y <= 9999 ? '' + y : '+' + y;
    });

    addFormatToken(0, ['YY', 2], 0, function () {
        return this.year() % 100;
    });

    addFormatToken(0, ['YYYY',   4],       0, 'year');
    addFormatToken(0, ['YYYYY',  5],       0, 'year');
    addFormatToken(0, ['YYYYYY', 6, true], 0, 'year');

    // ALIASES

    addUnitAlias('year', 'y');

    // PRIORITIES

    addUnitPriority('year', 1);

    // PARSING

    addRegexToken('Y',      matchSigned);
    addRegexToken('YY',     match1to2, match2);
    addRegexToken('YYYY',   match1to4, match4);
    addRegexToken('YYYYY',  match1to6, match6);
    addRegexToken('YYYYYY', match1to6, match6);

    addParseToken(['YYYYY', 'YYYYYY'], YEAR);
    addParseToken('YYYY', function (input, array) {
        array[YEAR] = input.length === 2 ? hooks.parseTwoDigitYear(input) : toInt(input);
    });
    addParseToken('YY', function (input, array) {
        array[YEAR] = hooks.parseTwoDigitYear(input);
    });
    addParseToken('Y', function (input, array) {
        array[YEAR] = parseInt(input, 10);
    });

    // HELPERS

    function daysInYear(year) {
        return isLeapYear(year) ? 366 : 365;
    }

    function isLeapYear(year) {
        return (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
    }

    // HOOKS

    hooks.parseTwoDigitYear = function (input) {
        return toInt(input) + (toInt(input) > 68 ? 1900 : 2000);
    };

    // MOMENTS

    var getSetYear = makeGetSet('FullYear', true);

    function getIsLeapYear () {
        return isLeapYear(this.year());
    }

    function makeGetSet (unit, keepTime) {
        return function (value) {
            if (value != null) {
                set$1(this, unit, value);
                hooks.updateOffset(this, keepTime);
                return this;
            } else {
                return get(this, unit);
            }
        };
    }

    function get (mom, unit) {
        return mom.isValid() ?
            mom._d['get' + (mom._isUTC ? 'UTC' : '') + unit]() : NaN;
    }

    function set$1 (mom, unit, value) {
        if (mom.isValid() && !isNaN(value)) {
            if (unit === 'FullYear' && isLeapYear(mom.year()) && mom.month() === 1 && mom.date() === 29) {
                mom._d['set' + (mom._isUTC ? 'UTC' : '') + unit](value, mom.month(), daysInMonth(value, mom.month()));
            }
            else {
                mom._d['set' + (mom._isUTC ? 'UTC' : '') + unit](value);
            }
        }
    }

    // MOMENTS

    function stringGet (units) {
        units = normalizeUnits(units);
        if (isFunction(this[units])) {
            return this[units]();
        }
        return this;
    }


    function stringSet (units, value) {
        if (typeof units === 'object') {
            units = normalizeObjectUnits(units);
            var prioritized = getPrioritizedUnits(units);
            for (var i = 0; i < prioritized.length; i++) {
                this[prioritized[i].unit](units[prioritized[i].unit]);
            }
        } else {
            units = normalizeUnits(units);
            if (isFunction(this[units])) {
                return this[units](value);
            }
        }
        return this;
    }

    function mod(n, x) {
        return ((n % x) + x) % x;
    }

    var indexOf;

    if (Array.prototype.indexOf) {
        indexOf = Array.prototype.indexOf;
    } else {
        indexOf = function (o) {
            // I know
            var i;
            for (i = 0; i < this.length; ++i) {
                if (this[i] === o) {
                    return i;
                }
            }
            return -1;
        };
    }

    function daysInMonth(year, month) {
        if (isNaN(year) || isNaN(month)) {
            return NaN;
        }
        var modMonth = mod(month, 12);
        year += (month - modMonth) / 12;
        return modMonth === 1 ? (isLeapYear(year) ? 29 : 28) : (31 - modMonth % 7 % 2);
    }

    // FORMATTING

    addFormatToken('M', ['MM', 2], 'Mo', function () {
        return this.month() + 1;
    });

    addFormatToken('MMM', 0, 0, function (format) {
        return this.localeData().monthsShort(this, format);
    });

    addFormatToken('MMMM', 0, 0, function (format) {
        return this.localeData().months(this, format);
    });

    // ALIASES

    addUnitAlias('month', 'M');

    // PRIORITY

    addUnitPriority('month', 8);

    // PARSING

    addRegexToken('M',    match1to2);
    addRegexToken('MM',   match1to2, match2);
    addRegexToken('MMM',  function (isStrict, locale) {
        return locale.monthsShortRegex(isStrict);
    });
    addRegexToken('MMMM', function (isStrict, locale) {
        return locale.monthsRegex(isStrict);
    });

    addParseToken(['M', 'MM'], function (input, array) {
        array[MONTH] = toInt(input) - 1;
    });

    addParseToken(['MMM', 'MMMM'], function (input, array, config, token) {
        var month = config._locale.monthsParse(input, token, config._strict);
        // if we didn't find a month name, mark the date as invalid.
        if (month != null) {
            array[MONTH] = month;
        } else {
            getParsingFlags(config).invalidMonth = input;
        }
    });

    // LOCALES

    var MONTHS_IN_FORMAT = /D[oD]?(\[[^\[\]]*\]|\s)+MMMM?/;
    var defaultLocaleMonths = 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_');
    function localeMonths (m, format) {
        if (!m) {
            return isArray(this._months) ? this._months :
                this._months['standalone'];
        }
        return isArray(this._months) ? this._months[m.month()] :
            this._months[(this._months.isFormat || MONTHS_IN_FORMAT).test(format) ? 'format' : 'standalone'][m.month()];
    }

    var defaultLocaleMonthsShort = 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_');
    function localeMonthsShort (m, format) {
        if (!m) {
            return isArray(this._monthsShort) ? this._monthsShort :
                this._monthsShort['standalone'];
        }
        return isArray(this._monthsShort) ? this._monthsShort[m.month()] :
            this._monthsShort[MONTHS_IN_FORMAT.test(format) ? 'format' : 'standalone'][m.month()];
    }

    function handleStrictParse(monthName, format, strict) {
        var i, ii, mom, llc = monthName.toLocaleLowerCase();
        if (!this._monthsParse) {
            // this is not used
            this._monthsParse = [];
            this._longMonthsParse = [];
            this._shortMonthsParse = [];
            for (i = 0; i < 12; ++i) {
                mom = createUTC([2000, i]);
                this._shortMonthsParse[i] = this.monthsShort(mom, '').toLocaleLowerCase();
                this._longMonthsParse[i] = this.months(mom, '').toLocaleLowerCase();
            }
        }

        if (strict) {
            if (format === 'MMM') {
                ii = indexOf.call(this._shortMonthsParse, llc);
                return ii !== -1 ? ii : null;
            } else {
                ii = indexOf.call(this._longMonthsParse, llc);
                return ii !== -1 ? ii : null;
            }
        } else {
            if (format === 'MMM') {
                ii = indexOf.call(this._shortMonthsParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._longMonthsParse, llc);
                return ii !== -1 ? ii : null;
            } else {
                ii = indexOf.call(this._longMonthsParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._shortMonthsParse, llc);
                return ii !== -1 ? ii : null;
            }
        }
    }

    function localeMonthsParse (monthName, format, strict) {
        var i, mom, regex;

        if (this._monthsParseExact) {
            return handleStrictParse.call(this, monthName, format, strict);
        }

        if (!this._monthsParse) {
            this._monthsParse = [];
            this._longMonthsParse = [];
            this._shortMonthsParse = [];
        }

        // TODO: add sorting
        // Sorting makes sure if one month (or abbr) is a prefix of another
        // see sorting in computeMonthsParse
        for (i = 0; i < 12; i++) {
            // make the regex if we don't have it already
            mom = createUTC([2000, i]);
            if (strict && !this._longMonthsParse[i]) {
                this._longMonthsParse[i] = new RegExp('^' + this.months(mom, '').replace('.', '') + '$', 'i');
                this._shortMonthsParse[i] = new RegExp('^' + this.monthsShort(mom, '').replace('.', '') + '$', 'i');
            }
            if (!strict && !this._monthsParse[i]) {
                regex = '^' + this.months(mom, '') + '|^' + this.monthsShort(mom, '');
                this._monthsParse[i] = new RegExp(regex.replace('.', ''), 'i');
            }
            // test the regex
            if (strict && format === 'MMMM' && this._longMonthsParse[i].test(monthName)) {
                return i;
            } else if (strict && format === 'MMM' && this._shortMonthsParse[i].test(monthName)) {
                return i;
            } else if (!strict && this._monthsParse[i].test(monthName)) {
                return i;
            }
        }
    }

    // MOMENTS

    function setMonth (mom, value) {
        var dayOfMonth;

        if (!mom.isValid()) {
            // No op
            return mom;
        }

        if (typeof value === 'string') {
            if (/^\d+$/.test(value)) {
                value = toInt(value);
            } else {
                value = mom.localeData().monthsParse(value);
                // TODO: Another silent failure?
                if (!isNumber(value)) {
                    return mom;
                }
            }
        }

        dayOfMonth = Math.min(mom.date(), daysInMonth(mom.year(), value));
        mom._d['set' + (mom._isUTC ? 'UTC' : '') + 'Month'](value, dayOfMonth);
        return mom;
    }

    function getSetMonth (value) {
        if (value != null) {
            setMonth(this, value);
            hooks.updateOffset(this, true);
            return this;
        } else {
            return get(this, 'Month');
        }
    }

    function getDaysInMonth () {
        return daysInMonth(this.year(), this.month());
    }

    var defaultMonthsShortRegex = matchWord;
    function monthsShortRegex (isStrict) {
        if (this._monthsParseExact) {
            if (!hasOwnProp(this, '_monthsRegex')) {
                computeMonthsParse.call(this);
            }
            if (isStrict) {
                return this._monthsShortStrictRegex;
            } else {
                return this._monthsShortRegex;
            }
        } else {
            if (!hasOwnProp(this, '_monthsShortRegex')) {
                this._monthsShortRegex = defaultMonthsShortRegex;
            }
            return this._monthsShortStrictRegex && isStrict ?
                this._monthsShortStrictRegex : this._monthsShortRegex;
        }
    }

    var defaultMonthsRegex = matchWord;
    function monthsRegex (isStrict) {
        if (this._monthsParseExact) {
            if (!hasOwnProp(this, '_monthsRegex')) {
                computeMonthsParse.call(this);
            }
            if (isStrict) {
                return this._monthsStrictRegex;
            } else {
                return this._monthsRegex;
            }
        } else {
            if (!hasOwnProp(this, '_monthsRegex')) {
                this._monthsRegex = defaultMonthsRegex;
            }
            return this._monthsStrictRegex && isStrict ?
                this._monthsStrictRegex : this._monthsRegex;
        }
    }

    function computeMonthsParse () {
        function cmpLenRev(a, b) {
            return b.length - a.length;
        }

        var shortPieces = [], longPieces = [], mixedPieces = [],
            i, mom;
        for (i = 0; i < 12; i++) {
            // make the regex if we don't have it already
            mom = createUTC([2000, i]);
            shortPieces.push(this.monthsShort(mom, ''));
            longPieces.push(this.months(mom, ''));
            mixedPieces.push(this.months(mom, ''));
            mixedPieces.push(this.monthsShort(mom, ''));
        }
        // Sorting makes sure if one month (or abbr) is a prefix of another it
        // will match the longer piece.
        shortPieces.sort(cmpLenRev);
        longPieces.sort(cmpLenRev);
        mixedPieces.sort(cmpLenRev);
        for (i = 0; i < 12; i++) {
            shortPieces[i] = regexEscape(shortPieces[i]);
            longPieces[i] = regexEscape(longPieces[i]);
        }
        for (i = 0; i < 24; i++) {
            mixedPieces[i] = regexEscape(mixedPieces[i]);
        }

        this._monthsRegex = new RegExp('^(' + mixedPieces.join('|') + ')', 'i');
        this._monthsShortRegex = this._monthsRegex;
        this._monthsStrictRegex = new RegExp('^(' + longPieces.join('|') + ')', 'i');
        this._monthsShortStrictRegex = new RegExp('^(' + shortPieces.join('|') + ')', 'i');
    }

    function createDate (y, m, d, h, M, s, ms) {
        // can't just apply() to create a date:
        // https://stackoverflow.com/q/181348
        var date;
        // the date constructor remaps years 0-99 to 1900-1999
        if (y < 100 && y >= 0) {
            // preserve leap years using a full 400 year cycle, then reset
            date = new Date(y + 400, m, d, h, M, s, ms);
            if (isFinite(date.getFullYear())) {
                date.setFullYear(y);
            }
        } else {
            date = new Date(y, m, d, h, M, s, ms);
        }

        return date;
    }

    function createUTCDate (y) {
        var date;
        // the Date.UTC function remaps years 0-99 to 1900-1999
        if (y < 100 && y >= 0) {
            var args = Array.prototype.slice.call(arguments);
            // preserve leap years using a full 400 year cycle, then reset
            args[0] = y + 400;
            date = new Date(Date.UTC.apply(null, args));
            if (isFinite(date.getUTCFullYear())) {
                date.setUTCFullYear(y);
            }
        } else {
            date = new Date(Date.UTC.apply(null, arguments));
        }

        return date;
    }

    // start-of-first-week - start-of-year
    function firstWeekOffset(year, dow, doy) {
        var // first-week day -- which january is always in the first week (4 for iso, 1 for other)
            fwd = 7 + dow - doy,
            // first-week day local weekday -- which local weekday is fwd
            fwdlw = (7 + createUTCDate(year, 0, fwd).getUTCDay() - dow) % 7;

        return -fwdlw + fwd - 1;
    }

    // https://en.wikipedia.org/wiki/ISO_week_date#Calculating_a_date_given_the_year.2C_week_number_and_weekday
    function dayOfYearFromWeeks(year, week, weekday, dow, doy) {
        var localWeekday = (7 + weekday - dow) % 7,
            weekOffset = firstWeekOffset(year, dow, doy),
            dayOfYear = 1 + 7 * (week - 1) + localWeekday + weekOffset,
            resYear, resDayOfYear;

        if (dayOfYear <= 0) {
            resYear = year - 1;
            resDayOfYear = daysInYear(resYear) + dayOfYear;
        } else if (dayOfYear > daysInYear(year)) {
            resYear = year + 1;
            resDayOfYear = dayOfYear - daysInYear(year);
        } else {
            resYear = year;
            resDayOfYear = dayOfYear;
        }

        return {
            year: resYear,
            dayOfYear: resDayOfYear
        };
    }

    function weekOfYear(mom, dow, doy) {
        var weekOffset = firstWeekOffset(mom.year(), dow, doy),
            week = Math.floor((mom.dayOfYear() - weekOffset - 1) / 7) + 1,
            resWeek, resYear;

        if (week < 1) {
            resYear = mom.year() - 1;
            resWeek = week + weeksInYear(resYear, dow, doy);
        } else if (week > weeksInYear(mom.year(), dow, doy)) {
            resWeek = week - weeksInYear(mom.year(), dow, doy);
            resYear = mom.year() + 1;
        } else {
            resYear = mom.year();
            resWeek = week;
        }

        return {
            week: resWeek,
            year: resYear
        };
    }

    function weeksInYear(year, dow, doy) {
        var weekOffset = firstWeekOffset(year, dow, doy),
            weekOffsetNext = firstWeekOffset(year + 1, dow, doy);
        return (daysInYear(year) - weekOffset + weekOffsetNext) / 7;
    }

    // FORMATTING

    addFormatToken('w', ['ww', 2], 'wo', 'week');
    addFormatToken('W', ['WW', 2], 'Wo', 'isoWeek');

    // ALIASES

    addUnitAlias('week', 'w');
    addUnitAlias('isoWeek', 'W');

    // PRIORITIES

    addUnitPriority('week', 5);
    addUnitPriority('isoWeek', 5);

    // PARSING

    addRegexToken('w',  match1to2);
    addRegexToken('ww', match1to2, match2);
    addRegexToken('W',  match1to2);
    addRegexToken('WW', match1to2, match2);

    addWeekParseToken(['w', 'ww', 'W', 'WW'], function (input, week, config, token) {
        week[token.substr(0, 1)] = toInt(input);
    });

    // HELPERS

    // LOCALES

    function localeWeek (mom) {
        return weekOfYear(mom, this._week.dow, this._week.doy).week;
    }

    var defaultLocaleWeek = {
        dow : 0, // Sunday is the first day of the week.
        doy : 6  // The week that contains Jan 6th is the first week of the year.
    };

    function localeFirstDayOfWeek () {
        return this._week.dow;
    }

    function localeFirstDayOfYear () {
        return this._week.doy;
    }

    // MOMENTS

    function getSetWeek (input) {
        var week = this.localeData().week(this);
        return input == null ? week : this.add((input - week) * 7, 'd');
    }

    function getSetISOWeek (input) {
        var week = weekOfYear(this, 1, 4).week;
        return input == null ? week : this.add((input - week) * 7, 'd');
    }

    // FORMATTING

    addFormatToken('d', 0, 'do', 'day');

    addFormatToken('dd', 0, 0, function (format) {
        return this.localeData().weekdaysMin(this, format);
    });

    addFormatToken('ddd', 0, 0, function (format) {
        return this.localeData().weekdaysShort(this, format);
    });

    addFormatToken('dddd', 0, 0, function (format) {
        return this.localeData().weekdays(this, format);
    });

    addFormatToken('e', 0, 0, 'weekday');
    addFormatToken('E', 0, 0, 'isoWeekday');

    // ALIASES

    addUnitAlias('day', 'd');
    addUnitAlias('weekday', 'e');
    addUnitAlias('isoWeekday', 'E');

    // PRIORITY
    addUnitPriority('day', 11);
    addUnitPriority('weekday', 11);
    addUnitPriority('isoWeekday', 11);

    // PARSING

    addRegexToken('d',    match1to2);
    addRegexToken('e',    match1to2);
    addRegexToken('E',    match1to2);
    addRegexToken('dd',   function (isStrict, locale) {
        return locale.weekdaysMinRegex(isStrict);
    });
    addRegexToken('ddd',   function (isStrict, locale) {
        return locale.weekdaysShortRegex(isStrict);
    });
    addRegexToken('dddd',   function (isStrict, locale) {
        return locale.weekdaysRegex(isStrict);
    });

    addWeekParseToken(['dd', 'ddd', 'dddd'], function (input, week, config, token) {
        var weekday = config._locale.weekdaysParse(input, token, config._strict);
        // if we didn't get a weekday name, mark the date as invalid
        if (weekday != null) {
            week.d = weekday;
        } else {
            getParsingFlags(config).invalidWeekday = input;
        }
    });

    addWeekParseToken(['d', 'e', 'E'], function (input, week, config, token) {
        week[token] = toInt(input);
    });

    // HELPERS

    function parseWeekday(input, locale) {
        if (typeof input !== 'string') {
            return input;
        }

        if (!isNaN(input)) {
            return parseInt(input, 10);
        }

        input = locale.weekdaysParse(input);
        if (typeof input === 'number') {
            return input;
        }

        return null;
    }

    function parseIsoWeekday(input, locale) {
        if (typeof input === 'string') {
            return locale.weekdaysParse(input) % 7 || 7;
        }
        return isNaN(input) ? null : input;
    }

    // LOCALES
    function shiftWeekdays (ws, n) {
        return ws.slice(n, 7).concat(ws.slice(0, n));
    }

    var defaultLocaleWeekdays = 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_');
    function localeWeekdays (m, format) {
        var weekdays = isArray(this._weekdays) ? this._weekdays :
            this._weekdays[(m && m !== true && this._weekdays.isFormat.test(format)) ? 'format' : 'standalone'];
        return (m === true) ? shiftWeekdays(weekdays, this._week.dow)
            : (m) ? weekdays[m.day()] : weekdays;
    }

    var defaultLocaleWeekdaysShort = 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_');
    function localeWeekdaysShort (m) {
        return (m === true) ? shiftWeekdays(this._weekdaysShort, this._week.dow)
            : (m) ? this._weekdaysShort[m.day()] : this._weekdaysShort;
    }

    var defaultLocaleWeekdaysMin = 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_');
    function localeWeekdaysMin (m) {
        return (m === true) ? shiftWeekdays(this._weekdaysMin, this._week.dow)
            : (m) ? this._weekdaysMin[m.day()] : this._weekdaysMin;
    }

    function handleStrictParse$1(weekdayName, format, strict) {
        var i, ii, mom, llc = weekdayName.toLocaleLowerCase();
        if (!this._weekdaysParse) {
            this._weekdaysParse = [];
            this._shortWeekdaysParse = [];
            this._minWeekdaysParse = [];

            for (i = 0; i < 7; ++i) {
                mom = createUTC([2000, 1]).day(i);
                this._minWeekdaysParse[i] = this.weekdaysMin(mom, '').toLocaleLowerCase();
                this._shortWeekdaysParse[i] = this.weekdaysShort(mom, '').toLocaleLowerCase();
                this._weekdaysParse[i] = this.weekdays(mom, '').toLocaleLowerCase();
            }
        }

        if (strict) {
            if (format === 'dddd') {
                ii = indexOf.call(this._weekdaysParse, llc);
                return ii !== -1 ? ii : null;
            } else if (format === 'ddd') {
                ii = indexOf.call(this._shortWeekdaysParse, llc);
                return ii !== -1 ? ii : null;
            } else {
                ii = indexOf.call(this._minWeekdaysParse, llc);
                return ii !== -1 ? ii : null;
            }
        } else {
            if (format === 'dddd') {
                ii = indexOf.call(this._weekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._shortWeekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._minWeekdaysParse, llc);
                return ii !== -1 ? ii : null;
            } else if (format === 'ddd') {
                ii = indexOf.call(this._shortWeekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._weekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._minWeekdaysParse, llc);
                return ii !== -1 ? ii : null;
            } else {
                ii = indexOf.call(this._minWeekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._weekdaysParse, llc);
                if (ii !== -1) {
                    return ii;
                }
                ii = indexOf.call(this._shortWeekdaysParse, llc);
                return ii !== -1 ? ii : null;
            }
        }
    }

    function localeWeekdaysParse (weekdayName, format, strict) {
        var i, mom, regex;

        if (this._weekdaysParseExact) {
            return handleStrictParse$1.call(this, weekdayName, format, strict);
        }

        if (!this._weekdaysParse) {
            this._weekdaysParse = [];
            this._minWeekdaysParse = [];
            this._shortWeekdaysParse = [];
            this._fullWeekdaysParse = [];
        }

        for (i = 0; i < 7; i++) {
            // make the regex if we don't have it already

            mom = createUTC([2000, 1]).day(i);
            if (strict && !this._fullWeekdaysParse[i]) {
                this._fullWeekdaysParse[i] = new RegExp('^' + this.weekdays(mom, '').replace('.', '\\.?') + '$', 'i');
                this._shortWeekdaysParse[i] = new RegExp('^' + this.weekdaysShort(mom, '').replace('.', '\\.?') + '$', 'i');
                this._minWeekdaysParse[i] = new RegExp('^' + this.weekdaysMin(mom, '').replace('.', '\\.?') + '$', 'i');
            }
            if (!this._weekdaysParse[i]) {
                regex = '^' + this.weekdays(mom, '') + '|^' + this.weekdaysShort(mom, '') + '|^' + this.weekdaysMin(mom, '');
                this._weekdaysParse[i] = new RegExp(regex.replace('.', ''), 'i');
            }
            // test the regex
            if (strict && format === 'dddd' && this._fullWeekdaysParse[i].test(weekdayName)) {
                return i;
            } else if (strict && format === 'ddd' && this._shortWeekdaysParse[i].test(weekdayName)) {
                return i;
            } else if (strict && format === 'dd' && this._minWeekdaysParse[i].test(weekdayName)) {
                return i;
            } else if (!strict && this._weekdaysParse[i].test(weekdayName)) {
                return i;
            }
        }
    }

    // MOMENTS

    function getSetDayOfWeek (input) {
        if (!this.isValid()) {
            return input != null ? this : NaN;
        }
        var day = this._isUTC ? this._d.getUTCDay() : this._d.getDay();
        if (input != null) {
            input = parseWeekday(input, this.localeData());
            return this.add(input - day, 'd');
        } else {
            return day;
        }
    }

    function getSetLocaleDayOfWeek (input) {
        if (!this.isValid()) {
            return input != null ? this : NaN;
        }
        var weekday = (this.day() + 7 - this.localeData()._week.dow) % 7;
        return input == null ? weekday : this.add(input - weekday, 'd');
    }

    function getSetISODayOfWeek (input) {
        if (!this.isValid()) {
            return input != null ? this : NaN;
        }

        // behaves the same as moment#day except
        // as a getter, returns 7 instead of 0 (1-7 range instead of 0-6)
        // as a setter, sunday should belong to the previous week.

        if (input != null) {
            var weekday = parseIsoWeekday(input, this.localeData());
            return this.day(this.day() % 7 ? weekday : weekday - 7);
        } else {
            return this.day() || 7;
        }
    }

    var defaultWeekdaysRegex = matchWord;
    function weekdaysRegex (isStrict) {
        if (this._weekdaysParseExact) {
            if (!hasOwnProp(this, '_weekdaysRegex')) {
                computeWeekdaysParse.call(this);
            }
            if (isStrict) {
                return this._weekdaysStrictRegex;
            } else {
                return this._weekdaysRegex;
            }
        } else {
            if (!hasOwnProp(this, '_weekdaysRegex')) {
                this._weekdaysRegex = defaultWeekdaysRegex;
            }
            return this._weekdaysStrictRegex && isStrict ?
                this._weekdaysStrictRegex : this._weekdaysRegex;
        }
    }

    var defaultWeekdaysShortRegex = matchWord;
    function weekdaysShortRegex (isStrict) {
        if (this._weekdaysParseExact) {
            if (!hasOwnProp(this, '_weekdaysRegex')) {
                computeWeekdaysParse.call(this);
            }
            if (isStrict) {
                return this._weekdaysShortStrictRegex;
            } else {
                return this._weekdaysShortRegex;
            }
        } else {
            if (!hasOwnProp(this, '_weekdaysShortRegex')) {
                this._weekdaysShortRegex = defaultWeekdaysShortRegex;
            }
            return this._weekdaysShortStrictRegex && isStrict ?
                this._weekdaysShortStrictRegex : this._weekdaysShortRegex;
        }
    }

    var defaultWeekdaysMinRegex = matchWord;
    function weekdaysMinRegex (isStrict) {
        if (this._weekdaysParseExact) {
            if (!hasOwnProp(this, '_weekdaysRegex')) {
                computeWeekdaysParse.call(this);
            }
            if (isStrict) {
                return this._weekdaysMinStrictRegex;
            } else {
                return this._weekdaysMinRegex;
            }
        } else {
            if (!hasOwnProp(this, '_weekdaysMinRegex')) {
                this._weekdaysMinRegex = defaultWeekdaysMinRegex;
            }
            return this._weekdaysMinStrictRegex && isStrict ?
                this._weekdaysMinStrictRegex : this._weekdaysMinRegex;
        }
    }


    function computeWeekdaysParse () {
        function cmpLenRev(a, b) {
            return b.length - a.length;
        }

        var minPieces = [], shortPieces = [], longPieces = [], mixedPieces = [],
            i, mom, minp, shortp, longp;
        for (i = 0; i < 7; i++) {
            // make the regex if we don't have it already
            mom = createUTC([2000, 1]).day(i);
            minp = this.weekdaysMin(mom, '');
            shortp = this.weekdaysShort(mom, '');
            longp = this.weekdays(mom, '');
            minPieces.push(minp);
            shortPieces.push(shortp);
            longPieces.push(longp);
            mixedPieces.push(minp);
            mixedPieces.push(shortp);
            mixedPieces.push(longp);
        }
        // Sorting makes sure if one weekday (or abbr) is a prefix of another it
        // will match the longer piece.
        minPieces.sort(cmpLenRev);
        shortPieces.sort(cmpLenRev);
        longPieces.sort(cmpLenRev);
        mixedPieces.sort(cmpLenRev);
        for (i = 0; i < 7; i++) {
            shortPieces[i] = regexEscape(shortPieces[i]);
            longPieces[i] = regexEscape(longPieces[i]);
            mixedPieces[i] = regexEscape(mixedPieces[i]);
        }

        this._weekdaysRegex = new RegExp('^(' + mixedPieces.join('|') + ')', 'i');
        this._weekdaysShortRegex = this._weekdaysRegex;
        this._weekdaysMinRegex = this._weekdaysRegex;

        this._weekdaysStrictRegex = new RegExp('^(' + longPieces.join('|') + ')', 'i');
        this._weekdaysShortStrictRegex = new RegExp('^(' + shortPieces.join('|') + ')', 'i');
        this._weekdaysMinStrictRegex = new RegExp('^(' + minPieces.join('|') + ')', 'i');
    }

    // FORMATTING

    function hFormat() {
        return this.hours() % 12 || 12;
    }

    function kFormat() {
        return this.hours() || 24;
    }

    addFormatToken('H', ['HH', 2], 0, 'hour');
    addFormatToken('h', ['hh', 2], 0, hFormat);
    addFormatToken('k', ['kk', 2], 0, kFormat);

    addFormatToken('hmm', 0, 0, function () {
        return '' + hFormat.apply(this) + zeroFill(this.minutes(), 2);
    });

    addFormatToken('hmmss', 0, 0, function () {
        return '' + hFormat.apply(this) + zeroFill(this.minutes(), 2) +
            zeroFill(this.seconds(), 2);
    });

    addFormatToken('Hmm', 0, 0, function () {
        return '' + this.hours() + zeroFill(this.minutes(), 2);
    });

    addFormatToken('Hmmss', 0, 0, function () {
        return '' + this.hours() + zeroFill(this.minutes(), 2) +
            zeroFill(this.seconds(), 2);
    });

    function meridiem (token, lowercase) {
        addFormatToken(token, 0, 0, function () {
            return this.localeData().meridiem(this.hours(), this.minutes(), lowercase);
        });
    }

    meridiem('a', true);
    meridiem('A', false);

    // ALIASES

    addUnitAlias('hour', 'h');

    // PRIORITY
    addUnitPriority('hour', 13);

    // PARSING

    function matchMeridiem (isStrict, locale) {
        return locale._meridiemParse;
    }

    addRegexToken('a',  matchMeridiem);
    addRegexToken('A',  matchMeridiem);
    addRegexToken('H',  match1to2);
    addRegexToken('h',  match1to2);
    addRegexToken('k',  match1to2);
    addRegexToken('HH', match1to2, match2);
    addRegexToken('hh', match1to2, match2);
    addRegexToken('kk', match1to2, match2);

    addRegexToken('hmm', match3to4);
    addRegexToken('hmmss', match5to6);
    addRegexToken('Hmm', match3to4);
    addRegexToken('Hmmss', match5to6);

    addParseToken(['H', 'HH'], HOUR);
    addParseToken(['k', 'kk'], function (input, array, config) {
        var kInput = toInt(input);
        array[HOUR] = kInput === 24 ? 0 : kInput;
    });
    addParseToken(['a', 'A'], function (input, array, config) {
        config._isPm = config._locale.isPM(input);
        config._meridiem = input;
    });
    addParseToken(['h', 'hh'], function (input, array, config) {
        array[HOUR] = toInt(input);
        getParsingFlags(config).bigHour = true;
    });
    addParseToken('hmm', function (input, array, config) {
        var pos = input.length - 2;
        array[HOUR] = toInt(input.substr(0, pos));
        array[MINUTE] = toInt(input.substr(pos));
        getParsingFlags(config).bigHour = true;
    });
    addParseToken('hmmss', function (input, array, config) {
        var pos1 = input.length - 4;
        var pos2 = input.length - 2;
        array[HOUR] = toInt(input.substr(0, pos1));
        array[MINUTE] = toInt(input.substr(pos1, 2));
        array[SECOND] = toInt(input.substr(pos2));
        getParsingFlags(config).bigHour = true;
    });
    addParseToken('Hmm', function (input, array, config) {
        var pos = input.length - 2;
        array[HOUR] = toInt(input.substr(0, pos));
        array[MINUTE] = toInt(input.substr(pos));
    });
    addParseToken('Hmmss', function (input, array, config) {
        var pos1 = input.length - 4;
        var pos2 = input.length - 2;
        array[HOUR] = toInt(input.substr(0, pos1));
        array[MINUTE] = toInt(input.substr(pos1, 2));
        array[SECOND] = toInt(input.substr(pos2));
    });

    // LOCALES

    function localeIsPM (input) {
        // IE8 Quirks Mode & IE7 Standards Mode do not allow accessing strings like arrays
        // Using charAt should be more compatible.
        return ((input + '').toLowerCase().charAt(0) === 'p');
    }

    var defaultLocaleMeridiemParse = /[ap]\.?m?\.?/i;
    function localeMeridiem (hours, minutes, isLower) {
        if (hours > 11) {
            return isLower ? 'pm' : 'PM';
        } else {
            return isLower ? 'am' : 'AM';
        }
    }


    // MOMENTS

    // Setting the hour should keep the time, because the user explicitly
    // specified which hour they want. So trying to maintain the same hour (in
    // a new timezone) makes sense. Adding/subtracting hours does not follow
    // this rule.
    var getSetHour = makeGetSet('Hours', true);

    var baseConfig = {
        calendar: defaultCalendar,
        longDateFormat: defaultLongDateFormat,
        invalidDate: defaultInvalidDate,
        ordinal: defaultOrdinal,
        dayOfMonthOrdinalParse: defaultDayOfMonthOrdinalParse,
        relativeTime: defaultRelativeTime,

        months: defaultLocaleMonths,
        monthsShort: defaultLocaleMonthsShort,

        week: defaultLocaleWeek,

        weekdays: defaultLocaleWeekdays,
        weekdaysMin: defaultLocaleWeekdaysMin,
        weekdaysShort: defaultLocaleWeekdaysShort,

        meridiemParse: defaultLocaleMeridiemParse
    };

    // internal storage for locale config files
    var locales = {};
    var localeFamilies = {};
    var globalLocale;

    function normalizeLocale(key) {
        return key ? key.toLowerCase().replace('_', '-') : key;
    }

    // pick the locale from the array
    // try ['en-au', 'en-gb'] as 'en-au', 'en-gb', 'en', as in move through the list trying each
    // substring from most specific to least, but move to the next array item if it's a more specific variant than the current root
    function chooseLocale(names) {
        var i = 0, j, next, locale, split;

        while (i < names.length) {
            split = normalizeLocale(names[i]).split('-');
            j = split.length;
            next = normalizeLocale(names[i + 1]);
            next = next ? next.split('-') : null;
            while (j > 0) {
                locale = loadLocale(split.slice(0, j).join('-'));
                if (locale) {
                    return locale;
                }
                if (next && next.length >= j && compareArrays(split, next, true) >= j - 1) {
                    //the next array item is better than a shallower substring of this one
                    break;
                }
                j--;
            }
            i++;
        }
        return globalLocale;
    }

    function loadLocale(name) {
        var oldLocale = null;
        // TODO: Find a better way to register and load all the locales in Node
        if (!locales[name] && (typeof module !== 'undefined') &&
                module && module.exports) {
            try {
                oldLocale = globalLocale._abbr;
                var aliasedRequire = require;
                aliasedRequire('./locale/' + name);
                getSetGlobalLocale(oldLocale);
            } catch (e) {}
        }
        return locales[name];
    }

    // This function will load locale and then set the global locale.  If
    // no arguments are passed in, it will simply return the current global
    // locale key.
    function getSetGlobalLocale (key, values) {
        var data;
        if (key) {
            if (isUndefined(values)) {
                data = getLocale(key);
            }
            else {
                data = defineLocale(key, values);
            }

            if (data) {
                // moment.duration._locale = moment._locale = data;
                globalLocale = data;
            }
            else {
                if ((typeof console !==  'undefined') && console.warn) {
                    //warn user if arguments are passed but the locale could not be set
                    console.warn('Locale ' + key +  ' not found. Did you forget to load it?');
                }
            }
        }

        return globalLocale._abbr;
    }

    function defineLocale (name, config) {
        if (config !== null) {
            var locale, parentConfig = baseConfig;
            config.abbr = name;
            if (locales[name] != null) {
                deprecateSimple('defineLocaleOverride',
                        'use moment.updateLocale(localeName, config) to change ' +
                        'an existing locale. moment.defineLocale(localeName, ' +
                        'config) should only be used for creating a new locale ' +
                        'See http://momentjs.com/guides/#/warnings/define-locale/ for more info.');
                parentConfig = locales[name]._config;
            } else if (config.parentLocale != null) {
                if (locales[config.parentLocale] != null) {
                    parentConfig = locales[config.parentLocale]._config;
                } else {
                    locale = loadLocale(config.parentLocale);
                    if (locale != null) {
                        parentConfig = locale._config;
                    } else {
                        if (!localeFamilies[config.parentLocale]) {
                            localeFamilies[config.parentLocale] = [];
                        }
                        localeFamilies[config.parentLocale].push({
                            name: name,
                            config: config
                        });
                        return null;
                    }
                }
            }
            locales[name] = new Locale(mergeConfigs(parentConfig, config));

            if (localeFamilies[name]) {
                localeFamilies[name].forEach(function (x) {
                    defineLocale(x.name, x.config);
                });
            }

            // backwards compat for now: also set the locale
            // make sure we set the locale AFTER all child locales have been
            // created, so we won't end up with the child locale set.
            getSetGlobalLocale(name);


            return locales[name];
        } else {
            // useful for testing
            delete locales[name];
            return null;
        }
    }

    function updateLocale(name, config) {
        if (config != null) {
            var locale, tmpLocale, parentConfig = baseConfig;
            // MERGE
            tmpLocale = loadLocale(name);
            if (tmpLocale != null) {
                parentConfig = tmpLocale._config;
            }
            config = mergeConfigs(parentConfig, config);
            locale = new Locale(config);
            locale.parentLocale = locales[name];
            locales[name] = locale;

            // backwards compat for now: also set the locale
            getSetGlobalLocale(name);
        } else {
            // pass null for config to unupdate, useful for tests
            if (locales[name] != null) {
                if (locales[name].parentLocale != null) {
                    locales[name] = locales[name].parentLocale;
                } else if (locales[name] != null) {
                    delete locales[name];
                }
            }
        }
        return locales[name];
    }

    // returns locale data
    function getLocale (key) {
        var locale;

        if (key && key._locale && key._locale._abbr) {
            key = key._locale._abbr;
        }

        if (!key) {
            return globalLocale;
        }

        if (!isArray(key)) {
            //short-circuit everything else
            locale = loadLocale(key);
            if (locale) {
                return locale;
            }
            key = [key];
        }

        return chooseLocale(key);
    }

    function listLocales() {
        return keys(locales);
    }

    function checkOverflow (m) {
        var overflow;
        var a = m._a;

        if (a && getParsingFlags(m).overflow === -2) {
            overflow =
                a[MONTH]       < 0 || a[MONTH]       > 11  ? MONTH :
                a[DATE]        < 1 || a[DATE]        > daysInMonth(a[YEAR], a[MONTH]) ? DATE :
                a[HOUR]        < 0 || a[HOUR]        > 24 || (a[HOUR] === 24 && (a[MINUTE] !== 0 || a[SECOND] !== 0 || a[MILLISECOND] !== 0)) ? HOUR :
                a[MINUTE]      < 0 || a[MINUTE]      > 59  ? MINUTE :
                a[SECOND]      < 0 || a[SECOND]      > 59  ? SECOND :
                a[MILLISECOND] < 0 || a[MILLISECOND] > 999 ? MILLISECOND :
                -1;

            if (getParsingFlags(m)._overflowDayOfYear && (overflow < YEAR || overflow > DATE)) {
                overflow = DATE;
            }
            if (getParsingFlags(m)._overflowWeeks && overflow === -1) {
                overflow = WEEK;
            }
            if (getParsingFlags(m)._overflowWeekday && overflow === -1) {
                overflow = WEEKDAY;
            }

            getParsingFlags(m).overflow = overflow;
        }

        return m;
    }

    // Pick the first defined of two or three arguments.
    function defaults(a, b, c) {
        if (a != null) {
            return a;
        }
        if (b != null) {
            return b;
        }
        return c;
    }

    function currentDateArray(config) {
        // hooks is actually the exported moment object
        var nowValue = new Date(hooks.now());
        if (config._useUTC) {
            return [nowValue.getUTCFullYear(), nowValue.getUTCMonth(), nowValue.getUTCDate()];
        }
        return [nowValue.getFullYear(), nowValue.getMonth(), nowValue.getDate()];
    }

    // convert an array to a date.
    // the array should mirror the parameters below
    // note: all values past the year are optional and will default to the lowest possible value.
    // [year, month, day , hour, minute, second, millisecond]
    function configFromArray (config) {
        var i, date, input = [], currentDate, expectedWeekday, yearToUse;

        if (config._d) {
            return;
        }

        currentDate = currentDateArray(config);

        //compute day of the year from weeks and weekdays
        if (config._w && config._a[DATE] == null && config._a[MONTH] == null) {
            dayOfYearFromWeekInfo(config);
        }

        //if the day of the year is set, figure out what it is
        if (config._dayOfYear != null) {
            yearToUse = defaults(config._a[YEAR], currentDate[YEAR]);

            if (config._dayOfYear > daysInYear(yearToUse) || config._dayOfYear === 0) {
                getParsingFlags(config)._overflowDayOfYear = true;
            }

            date = createUTCDate(yearToUse, 0, config._dayOfYear);
            config._a[MONTH] = date.getUTCMonth();
            config._a[DATE] = date.getUTCDate();
        }

        // Default to current date.
        // * if no year, month, day of month are given, default to today
        // * if day of month is given, default month and year
        // * if month is given, default only year
        // * if year is given, don't default anything
        for (i = 0; i < 3 && config._a[i] == null; ++i) {
            config._a[i] = input[i] = currentDate[i];
        }

        // Zero out whatever was not defaulted, including time
        for (; i < 7; i++) {
            config._a[i] = input[i] = (config._a[i] == null) ? (i === 2 ? 1 : 0) : config._a[i];
        }

        // Check for 24:00:00.000
        if (config._a[HOUR] === 24 &&
                config._a[MINUTE] === 0 &&
                config._a[SECOND] === 0 &&
                config._a[MILLISECOND] === 0) {
            config._nextDay = true;
            config._a[HOUR] = 0;
        }

        config._d = (config._useUTC ? createUTCDate : createDate).apply(null, input);
        expectedWeekday = config._useUTC ? config._d.getUTCDay() : config._d.getDay();

        // Apply timezone offset from input. The actual utcOffset can be changed
        // with parseZone.
        if (config._tzm != null) {
            config._d.setUTCMinutes(config._d.getUTCMinutes() - config._tzm);
        }

        if (config._nextDay) {
            config._a[HOUR] = 24;
        }

        // check for mismatching day of week
        if (config._w && typeof config._w.d !== 'undefined' && config._w.d !== expectedWeekday) {
            getParsingFlags(config).weekdayMismatch = true;
        }
    }

    function dayOfYearFromWeekInfo(config) {
        var w, weekYear, week, weekday, dow, doy, temp, weekdayOverflow;

        w = config._w;
        if (w.GG != null || w.W != null || w.E != null) {
            dow = 1;
            doy = 4;

            // TODO: We need to take the current isoWeekYear, but that depends on
            // how we interpret now (local, utc, fixed offset). So create
            // a now version of current config (take local/utc/offset flags, and
            // create now).
            weekYear = defaults(w.GG, config._a[YEAR], weekOfYear(createLocal(), 1, 4).year);
            week = defaults(w.W, 1);
            weekday = defaults(w.E, 1);
            if (weekday < 1 || weekday > 7) {
                weekdayOverflow = true;
            }
        } else {
            dow = config._locale._week.dow;
            doy = config._locale._week.doy;

            var curWeek = weekOfYear(createLocal(), dow, doy);

            weekYear = defaults(w.gg, config._a[YEAR], curWeek.year);

            // Default to current week.
            week = defaults(w.w, curWeek.week);

            if (w.d != null) {
                // weekday -- low day numbers are considered next week
                weekday = w.d;
                if (weekday < 0 || weekday > 6) {
                    weekdayOverflow = true;
                }
            } else if (w.e != null) {
                // local weekday -- counting starts from beginning of week
                weekday = w.e + dow;
                if (w.e < 0 || w.e > 6) {
                    weekdayOverflow = true;
                }
            } else {
                // default to beginning of week
                weekday = dow;
            }
        }
        if (week < 1 || week > weeksInYear(weekYear, dow, doy)) {
            getParsingFlags(config)._overflowWeeks = true;
        } else if (weekdayOverflow != null) {
            getParsingFlags(config)._overflowWeekday = true;
        } else {
            temp = dayOfYearFromWeeks(weekYear, week, weekday, dow, doy);
            config._a[YEAR] = temp.year;
            config._dayOfYear = temp.dayOfYear;
        }
    }

    // iso 8601 regex
    // 0000-00-00 0000-W00 or 0000-W00-0 + T + 00 or 00:00 or 00:00:00 or 00:00:00.000 + +00:00 or +0000 or +00)
    var extendedIsoRegex = /^\s*((?:[+-]\d{6}|\d{4})-(?:\d\d-\d\d|W\d\d-\d|W\d\d|\d\d\d|\d\d))(?:(T| )(\d\d(?::\d\d(?::\d\d(?:[.,]\d+)?)?)?)([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/;
    var basicIsoRegex = /^\s*((?:[+-]\d{6}|\d{4})(?:\d\d\d\d|W\d\d\d|W\d\d|\d\d\d|\d\d))(?:(T| )(\d\d(?:\d\d(?:\d\d(?:[.,]\d+)?)?)?)([\+\-]\d\d(?::?\d\d)?|\s*Z)?)?$/;

    var tzRegex = /Z|[+-]\d\d(?::?\d\d)?/;

    var isoDates = [
        ['YYYYYY-MM-DD', /[+-]\d{6}-\d\d-\d\d/],
        ['YYYY-MM-DD', /\d{4}-\d\d-\d\d/],
        ['GGGG-[W]WW-E', /\d{4}-W\d\d-\d/],
        ['GGGG-[W]WW', /\d{4}-W\d\d/, false],
        ['YYYY-DDD', /\d{4}-\d{3}/],
        ['YYYY-MM', /\d{4}-\d\d/, false],
        ['YYYYYYMMDD', /[+-]\d{10}/],
        ['YYYYMMDD', /\d{8}/],
        // YYYYMM is NOT allowed by the standard
        ['GGGG[W]WWE', /\d{4}W\d{3}/],
        ['GGGG[W]WW', /\d{4}W\d{2}/, false],
        ['YYYYDDD', /\d{7}/]
    ];

    // iso time formats and regexes
    var isoTimes = [
        ['HH:mm:ss.SSSS', /\d\d:\d\d:\d\d\.\d+/],
        ['HH:mm:ss,SSSS', /\d\d:\d\d:\d\d,\d+/],
        ['HH:mm:ss', /\d\d:\d\d:\d\d/],
        ['HH:mm', /\d\d:\d\d/],
        ['HHmmss.SSSS', /\d\d\d\d\d\d\.\d+/],
        ['HHmmss,SSSS', /\d\d\d\d\d\d,\d+/],
        ['HHmmss', /\d\d\d\d\d\d/],
        ['HHmm', /\d\d\d\d/],
        ['HH', /\d\d/]
    ];

    var aspNetJsonRegex = /^\/?Date\((\-?\d+)/i;

    // date from iso format
    function configFromISO(config) {
        var i, l,
            string = config._i,
            match = extendedIsoRegex.exec(string) || basicIsoRegex.exec(string),
            allowTime, dateFormat, timeFormat, tzFormat;

        if (match) {
            getParsingFlags(config).iso = true;

            for (i = 0, l = isoDates.length; i < l; i++) {
                if (isoDates[i][1].exec(match[1])) {
                    dateFormat = isoDates[i][0];
                    allowTime = isoDates[i][2] !== false;
                    break;
                }
            }
            if (dateFormat == null) {
                config._isValid = false;
                return;
            }
            if (match[3]) {
                for (i = 0, l = isoTimes.length; i < l; i++) {
                    if (isoTimes[i][1].exec(match[3])) {
                        // match[2] should be 'T' or space
                        timeFormat = (match[2] || ' ') + isoTimes[i][0];
                        break;
                    }
                }
                if (timeFormat == null) {
                    config._isValid = false;
                    return;
                }
            }
            if (!allowTime && timeFormat != null) {
                config._isValid = false;
                return;
            }
            if (match[4]) {
                if (tzRegex.exec(match[4])) {
                    tzFormat = 'Z';
                } else {
                    config._isValid = false;
                    return;
                }
            }
            config._f = dateFormat + (timeFormat || '') + (tzFormat || '');
            configFromStringAndFormat(config);
        } else {
            config._isValid = false;
        }
    }

    // RFC 2822 regex: For details see https://tools.ietf.org/html/rfc2822#section-3.3
    var rfc2822 = /^(?:(Mon|Tue|Wed|Thu|Fri|Sat|Sun),?\s)?(\d{1,2})\s(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s(\d{2,4})\s(\d\d):(\d\d)(?::(\d\d))?\s(?:(UT|GMT|[ECMP][SD]T)|([Zz])|([+-]\d{4}))$/;

    function extractFromRFC2822Strings(yearStr, monthStr, dayStr, hourStr, minuteStr, secondStr) {
        var result = [
            untruncateYear(yearStr),
            defaultLocaleMonthsShort.indexOf(monthStr),
            parseInt(dayStr, 10),
            parseInt(hourStr, 10),
            parseInt(minuteStr, 10)
        ];

        if (secondStr) {
            result.push(parseInt(secondStr, 10));
        }

        return result;
    }

    function untruncateYear(yearStr) {
        var year = parseInt(yearStr, 10);
        if (year <= 49) {
            return 2000 + year;
        } else if (year <= 999) {
            return 1900 + year;
        }
        return year;
    }

    function preprocessRFC2822(s) {
        // Remove comments and folding whitespace and replace multiple-spaces with a single space
        return s.replace(/\([^)]*\)|[\n\t]/g, ' ').replace(/(\s\s+)/g, ' ').replace(/^\s\s*/, '').replace(/\s\s*$/, '');
    }

    function checkWeekday(weekdayStr, parsedInput, config) {
        if (weekdayStr) {
            // TODO: Replace the vanilla JS Date object with an indepentent day-of-week check.
            var weekdayProvided = defaultLocaleWeekdaysShort.indexOf(weekdayStr),
                weekdayActual = new Date(parsedInput[0], parsedInput[1], parsedInput[2]).getDay();
            if (weekdayProvided !== weekdayActual) {
                getParsingFlags(config).weekdayMismatch = true;
                config._isValid = false;
                return false;
            }
        }
        return true;
    }

    var obsOffsets = {
        UT: 0,
        GMT: 0,
        EDT: -4 * 60,
        EST: -5 * 60,
        CDT: -5 * 60,
        CST: -6 * 60,
        MDT: -6 * 60,
        MST: -7 * 60,
        PDT: -7 * 60,
        PST: -8 * 60
    };

    function calculateOffset(obsOffset, militaryOffset, numOffset) {
        if (obsOffset) {
            return obsOffsets[obsOffset];
        } else if (militaryOffset) {
            // the only allowed military tz is Z
            return 0;
        } else {
            var hm = parseInt(numOffset, 10);
            var m = hm % 100, h = (hm - m) / 100;
            return h * 60 + m;
        }
    }

    // date and time from ref 2822 format
    function configFromRFC2822(config) {
        var match = rfc2822.exec(preprocessRFC2822(config._i));
        if (match) {
            var parsedArray = extractFromRFC2822Strings(match[4], match[3], match[2], match[5], match[6], match[7]);
            if (!checkWeekday(match[1], parsedArray, config)) {
                return;
            }

            config._a = parsedArray;
            config._tzm = calculateOffset(match[8], match[9], match[10]);

            config._d = createUTCDate.apply(null, config._a);
            config._d.setUTCMinutes(config._d.getUTCMinutes() - config._tzm);

            getParsingFlags(config).rfc2822 = true;
        } else {
            config._isValid = false;
        }
    }

    // date from iso format or fallback
    function configFromString(config) {
        var matched = aspNetJsonRegex.exec(config._i);

        if (matched !== null) {
            config._d = new Date(+matched[1]);
            return;
        }

        configFromISO(config);
        if (config._isValid === false) {
            delete config._isValid;
        } else {
            return;
        }

        configFromRFC2822(config);
        if (config._isValid === false) {
            delete config._isValid;
        } else {
            return;
        }

        // Final attempt, use Input Fallback
        hooks.createFromInputFallback(config);
    }

    hooks.createFromInputFallback = deprecate(
        'value provided is not in a recognized RFC2822 or ISO format. moment construction falls back to js Date(), ' +
        'which is not reliable across all browsers and versions. Non RFC2822/ISO date formats are ' +
        'discouraged and will be removed in an upcoming major release. Please refer to ' +
        'http://momentjs.com/guides/#/warnings/js-date/ for more info.',
        function (config) {
            config._d = new Date(config._i + (config._useUTC ? ' UTC' : ''));
        }
    );

    // constant that refers to the ISO standard
    hooks.ISO_8601 = function () {};

    // constant that refers to the RFC 2822 form
    hooks.RFC_2822 = function () {};

    // date from string and format string
    function configFromStringAndFormat(config) {
        // TODO: Move this to another part of the creation flow to prevent circular deps
        if (config._f === hooks.ISO_8601) {
            configFromISO(config);
            return;
        }
        if (config._f === hooks.RFC_2822) {
            configFromRFC2822(config);
            return;
        }
        config._a = [];
        getParsingFlags(config).empty = true;

        // This array is used to make a Date, either with `new Date` or `Date.UTC`
        var string = '' + config._i,
            i, parsedInput, tokens, token, skipped,
            stringLength = string.length,
            totalParsedInputLength = 0;

        tokens = expandFormat(config._f, config._locale).match(formattingTokens) || [];

        for (i = 0; i < tokens.length; i++) {
            token = tokens[i];
            parsedInput = (string.match(getParseRegexForToken(token, config)) || [])[0];
            // console.log('token', token, 'parsedInput', parsedInput,
            //         'regex', getParseRegexForToken(token, config));
            if (parsedInput) {
                skipped = string.substr(0, string.indexOf(parsedInput));
                if (skipped.length > 0) {
                    getParsingFlags(config).unusedInput.push(skipped);
                }
                string = string.slice(string.indexOf(parsedInput) + parsedInput.length);
                totalParsedInputLength += parsedInput.length;
            }
            // don't parse if it's not a known token
            if (formatTokenFunctions[token]) {
                if (parsedInput) {
                    getParsingFlags(config).empty = false;
                }
                else {
                    getParsingFlags(config).unusedTokens.push(token);
                }
                addTimeToArrayFromToken(token, parsedInput, config);
            }
            else if (config._strict && !parsedInput) {
                getParsingFlags(config).unusedTokens.push(token);
            }
        }

        // add remaining unparsed input length to the string
        getParsingFlags(config).charsLeftOver = stringLength - totalParsedInputLength;
        if (string.length > 0) {
            getParsingFlags(config).unusedInput.push(string);
        }

        // clear _12h flag if hour is <= 12
        if (config._a[HOUR] <= 12 &&
            getParsingFlags(config).bigHour === true &&
            config._a[HOUR] > 0) {
            getParsingFlags(config).bigHour = undefined;
        }

        getParsingFlags(config).parsedDateParts = config._a.slice(0);
        getParsingFlags(config).meridiem = config._meridiem;
        // handle meridiem
        config._a[HOUR] = meridiemFixWrap(config._locale, config._a[HOUR], config._meridiem);

        configFromArray(config);
        checkOverflow(config);
    }


    function meridiemFixWrap (locale, hour, meridiem) {
        var isPm;

        if (meridiem == null) {
            // nothing to do
            return hour;
        }
        if (locale.meridiemHour != null) {
            return locale.meridiemHour(hour, meridiem);
        } else if (locale.isPM != null) {
            // Fallback
            isPm = locale.isPM(meridiem);
            if (isPm && hour < 12) {
                hour += 12;
            }
            if (!isPm && hour === 12) {
                hour = 0;
            }
            return hour;
        } else {
            // this is not supposed to happen
            return hour;
        }
    }

    // date from string and array of format strings
    function configFromStringAndArray(config) {
        var tempConfig,
            bestMoment,

            scoreToBeat,
            i,
            currentScore;

        if (config._f.length === 0) {
            getParsingFlags(config).invalidFormat = true;
            config._d = new Date(NaN);
            return;
        }

        for (i = 0; i < config._f.length; i++) {
            currentScore = 0;
            tempConfig = copyConfig({}, config);
            if (config._useUTC != null) {
                tempConfig._useUTC = config._useUTC;
            }
            tempConfig._f = config._f[i];
            configFromStringAndFormat(tempConfig);

            if (!isValid(tempConfig)) {
                continue;
            }

            // if there is any input that was not parsed add a penalty for that format
            currentScore += getParsingFlags(tempConfig).charsLeftOver;

            //or tokens
            currentScore += getParsingFlags(tempConfig).unusedTokens.length * 10;

            getParsingFlags(tempConfig).score = currentScore;

            if (scoreToBeat == null || currentScore < scoreToBeat) {
                scoreToBeat = currentScore;
                bestMoment = tempConfig;
            }
        }

        extend(config, bestMoment || tempConfig);
    }

    function configFromObject(config) {
        if (config._d) {
            return;
        }

        var i = normalizeObjectUnits(config._i);
        config._a = map([i.year, i.month, i.day || i.date, i.hour, i.minute, i.second, i.millisecond], function (obj) {
            return obj && parseInt(obj, 10);
        });

        configFromArray(config);
    }

    function createFromConfig (config) {
        var res = new Moment(checkOverflow(prepareConfig(config)));
        if (res._nextDay) {
            // Adding is smart enough around DST
            res.add(1, 'd');
            res._nextDay = undefined;
        }

        return res;
    }

    function prepareConfig (config) {
        var input = config._i,
            format = config._f;

        config._locale = config._locale || getLocale(config._l);

        if (input === null || (format === undefined && input === '')) {
            return createInvalid({nullInput: true});
        }

        if (typeof input === 'string') {
            config._i = input = config._locale.preparse(input);
        }

        if (isMoment(input)) {
            return new Moment(checkOverflow(input));
        } else if (isDate(input)) {
            config._d = input;
        } else if (isArray(format)) {
            configFromStringAndArray(config);
        } else if (format) {
            configFromStringAndFormat(config);
        }  else {
            configFromInput(config);
        }

        if (!isValid(config)) {
            config._d = null;
        }

        return config;
    }

    function configFromInput(config) {
        var input = config._i;
        if (isUndefined(input)) {
            config._d = new Date(hooks.now());
        } else if (isDate(input)) {
            config._d = new Date(input.valueOf());
        } else if (typeof input === 'string') {
            configFromString(config);
        } else if (isArray(input)) {
            config._a = map(input.slice(0), function (obj) {
                return parseInt(obj, 10);
            });
            configFromArray(config);
        } else if (isObject(input)) {
            configFromObject(config);
        } else if (isNumber(input)) {
            // from milliseconds
            config._d = new Date(input);
        } else {
            hooks.createFromInputFallback(config);
        }
    }

    function createLocalOrUTC (input, format, locale, strict, isUTC) {
        var c = {};

        if (locale === true || locale === false) {
            strict = locale;
            locale = undefined;
        }

        if ((isObject(input) && isObjectEmpty(input)) ||
                (isArray(input) && input.length === 0)) {
            input = undefined;
        }
        // object construction must be done this way.
        // https://github.com/moment/moment/issues/1423
        c._isAMomentObject = true;
        c._useUTC = c._isUTC = isUTC;
        c._l = locale;
        c._i = input;
        c._f = format;
        c._strict = strict;

        return createFromConfig(c);
    }

    function createLocal (input, format, locale, strict) {
        return createLocalOrUTC(input, format, locale, strict, false);
    }

    var prototypeMin = deprecate(
        'moment().min is deprecated, use moment.max instead. http://momentjs.com/guides/#/warnings/min-max/',
        function () {
            var other = createLocal.apply(null, arguments);
            if (this.isValid() && other.isValid()) {
                return other < this ? this : other;
            } else {
                return createInvalid();
            }
        }
    );

    var prototypeMax = deprecate(
        'moment().max is deprecated, use moment.min instead. http://momentjs.com/guides/#/warnings/min-max/',
        function () {
            var other = createLocal.apply(null, arguments);
            if (this.isValid() && other.isValid()) {
                return other > this ? this : other;
            } else {
                return createInvalid();
            }
        }
    );

    // Pick a moment m from moments so that m[fn](other) is true for all
    // other. This relies on the function fn to be transitive.
    //
    // moments should either be an array of moment objects or an array, whose
    // first element is an array of moment objects.
    function pickBy(fn, moments) {
        var res, i;
        if (moments.length === 1 && isArray(moments[0])) {
            moments = moments[0];
        }
        if (!moments.length) {
            return createLocal();
        }
        res = moments[0];
        for (i = 1; i < moments.length; ++i) {
            if (!moments[i].isValid() || moments[i][fn](res)) {
                res = moments[i];
            }
        }
        return res;
    }

    // TODO: Use [].sort instead?
    function min () {
        var args = [].slice.call(arguments, 0);

        return pickBy('isBefore', args);
    }

    function max () {
        var args = [].slice.call(arguments, 0);

        return pickBy('isAfter', args);
    }

    var now = function () {
        return Date.now ? Date.now() : +(new Date());
    };

    var ordering = ['year', 'quarter', 'month', 'week', 'day', 'hour', 'minute', 'second', 'millisecond'];

    function isDurationValid(m) {
        for (var key in m) {
            if (!(indexOf.call(ordering, key) !== -1 && (m[key] == null || !isNaN(m[key])))) {
                return false;
            }
        }

        var unitHasDecimal = false;
        for (var i = 0; i < ordering.length; ++i) {
            if (m[ordering[i]]) {
                if (unitHasDecimal) {
                    return false; // only allow non-integers for smallest unit
                }
                if (parseFloat(m[ordering[i]]) !== toInt(m[ordering[i]])) {
                    unitHasDecimal = true;
                }
            }
        }

        return true;
    }

    function isValid$1() {
        return this._isValid;
    }

    function createInvalid$1() {
        return createDuration(NaN);
    }

    function Duration (duration) {
        var normalizedInput = normalizeObjectUnits(duration),
            years = normalizedInput.year || 0,
            quarters = normalizedInput.quarter || 0,
            months = normalizedInput.month || 0,
            weeks = normalizedInput.week || normalizedInput.isoWeek || 0,
            days = normalizedInput.day || 0,
            hours = normalizedInput.hour || 0,
            minutes = normalizedInput.minute || 0,
            seconds = normalizedInput.second || 0,
            milliseconds = normalizedInput.millisecond || 0;

        this._isValid = isDurationValid(normalizedInput);

        // representation for dateAddRemove
        this._milliseconds = +milliseconds +
            seconds * 1e3 + // 1000
            minutes * 6e4 + // 1000 * 60
            hours * 1000 * 60 * 60; //using 1000 * 60 * 60 instead of 36e5 to avoid floating point rounding errors https://github.com/moment/moment/issues/2978
        // Because of dateAddRemove treats 24 hours as different from a
        // day when working around DST, we need to store them separately
        this._days = +days +
            weeks * 7;
        // It is impossible to translate months into days without knowing
        // which months you are are talking about, so we have to store
        // it separately.
        this._months = +months +
            quarters * 3 +
            years * 12;

        this._data = {};

        this._locale = getLocale();

        this._bubble();
    }

    function isDuration (obj) {
        return obj instanceof Duration;
    }

    function absRound (number) {
        if (number < 0) {
            return Math.round(-1 * number) * -1;
        } else {
            return Math.round(number);
        }
    }

    // FORMATTING

    function offset (token, separator) {
        addFormatToken(token, 0, 0, function () {
            var offset = this.utcOffset();
            var sign = '+';
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            }
            return sign + zeroFill(~~(offset / 60), 2) + separator + zeroFill(~~(offset) % 60, 2);
        });
    }

    offset('Z', ':');
    offset('ZZ', '');

    // PARSING

    addRegexToken('Z',  matchShortOffset);
    addRegexToken('ZZ', matchShortOffset);
    addParseToken(['Z', 'ZZ'], function (input, array, config) {
        config._useUTC = true;
        config._tzm = offsetFromString(matchShortOffset, input);
    });

    // HELPERS

    // timezone chunker
    // '+10:00' > ['10',  '00']
    // '-1530'  > ['-15', '30']
    var chunkOffset = /([\+\-]|\d\d)/gi;

    function offsetFromString(matcher, string) {
        var matches = (string || '').match(matcher);

        if (matches === null) {
            return null;
        }

        var chunk   = matches[matches.length - 1] || [];
        var parts   = (chunk + '').match(chunkOffset) || ['-', 0, 0];
        var minutes = +(parts[1] * 60) + toInt(parts[2]);

        return minutes === 0 ?
          0 :
          parts[0] === '+' ? minutes : -minutes;
    }

    // Return a moment from input, that is local/utc/zone equivalent to model.
    function cloneWithOffset(input, model) {
        var res, diff;
        if (model._isUTC) {
            res = model.clone();
            diff = (isMoment(input) || isDate(input) ? input.valueOf() : createLocal(input).valueOf()) - res.valueOf();
            // Use low-level api, because this fn is low-level api.
            res._d.setTime(res._d.valueOf() + diff);
            hooks.updateOffset(res, false);
            return res;
        } else {
            return createLocal(input).local();
        }
    }

    function getDateOffset (m) {
        // On Firefox.24 Date#getTimezoneOffset returns a floating point.
        // https://github.com/moment/moment/pull/1871
        return -Math.round(m._d.getTimezoneOffset() / 15) * 15;
    }

    // HOOKS

    // This function will be called whenever a moment is mutated.
    // It is intended to keep the offset in sync with the timezone.
    hooks.updateOffset = function () {};

    // MOMENTS

    // keepLocalTime = true means only change the timezone, without
    // affecting the local hour. So 5:31:26 +0300 --[utcOffset(2, true)]-->
    // 5:31:26 +0200 It is possible that 5:31:26 doesn't exist with offset
    // +0200, so we adjust the time as needed, to be valid.
    //
    // Keeping the time actually adds/subtracts (one hour)
    // from the actual represented time. That is why we call updateOffset
    // a second time. In case it wants us to change the offset again
    // _changeInProgress == true case, then we have to adjust, because
    // there is no such time in the given timezone.
    function getSetOffset (input, keepLocalTime, keepMinutes) {
        var offset = this._offset || 0,
            localAdjust;
        if (!this.isValid()) {
            return input != null ? this : NaN;
        }
        if (input != null) {
            if (typeof input === 'string') {
                input = offsetFromString(matchShortOffset, input);
                if (input === null) {
                    return this;
                }
            } else if (Math.abs(input) < 16 && !keepMinutes) {
                input = input * 60;
            }
            if (!this._isUTC && keepLocalTime) {
                localAdjust = getDateOffset(this);
            }
            this._offset = input;
            this._isUTC = true;
            if (localAdjust != null) {
                this.add(localAdjust, 'm');
            }
            if (offset !== input) {
                if (!keepLocalTime || this._changeInProgress) {
                    addSubtract(this, createDuration(input - offset, 'm'), 1, false);
                } else if (!this._changeInProgress) {
                    this._changeInProgress = true;
                    hooks.updateOffset(this, true);
                    this._changeInProgress = null;
                }
            }
            return this;
        } else {
            return this._isUTC ? offset : getDateOffset(this);
        }
    }

    function getSetZone (input, keepLocalTime) {
        if (input != null) {
            if (typeof input !== 'string') {
                input = -input;
            }

            this.utcOffset(input, keepLocalTime);

            return this;
        } else {
            return -this.utcOffset();
        }
    }

    function setOffsetToUTC (keepLocalTime) {
        return this.utcOffset(0, keepLocalTime);
    }

    function setOffsetToLocal (keepLocalTime) {
        if (this._isUTC) {
            this.utcOffset(0, keepLocalTime);
            this._isUTC = false;

            if (keepLocalTime) {
                this.subtract(getDateOffset(this), 'm');
            }
        }
        return this;
    }

    function setOffsetToParsedOffset () {
        if (this._tzm != null) {
            this.utcOffset(this._tzm, false, true);
        } else if (typeof this._i === 'string') {
            var tZone = offsetFromString(matchOffset, this._i);
            if (tZone != null) {
                this.utcOffset(tZone);
            }
            else {
                this.utcOffset(0, true);
            }
        }
        return this;
    }

    function hasAlignedHourOffset (input) {
        if (!this.isValid()) {
            return false;
        }
        input = input ? createLocal(input).utcOffset() : 0;

        return (this.utcOffset() - input) % 60 === 0;
    }

    function isDaylightSavingTime () {
        return (
            this.utcOffset() > this.clone().month(0).utcOffset() ||
            this.utcOffset() > this.clone().month(5).utcOffset()
        );
    }

    function isDaylightSavingTimeShifted () {
        if (!isUndefined(this._isDSTShifted)) {
            return this._isDSTShifted;
        }

        var c = {};

        copyConfig(c, this);
        c = prepareConfig(c);

        if (c._a) {
            var other = c._isUTC ? createUTC(c._a) : createLocal(c._a);
            this._isDSTShifted = this.isValid() &&
                compareArrays(c._a, other.toArray()) > 0;
        } else {
            this._isDSTShifted = false;
        }

        return this._isDSTShifted;
    }

    function isLocal () {
        return this.isValid() ? !this._isUTC : false;
    }

    function isUtcOffset () {
        return this.isValid() ? this._isUTC : false;
    }

    function isUtc () {
        return this.isValid() ? this._isUTC && this._offset === 0 : false;
    }

    // ASP.NET json date format regex
    var aspNetRegex = /^(\-|\+)?(?:(\d*)[. ])?(\d+)\:(\d+)(?:\:(\d+)(\.\d*)?)?$/;

    // from http://docs.closure-library.googlecode.com/git/closure_goog_date_date.js.source.html
    // somewhat more in line with 4.4.3.2 2004 spec, but allows decimal anywhere
    // and further modified to allow for strings containing both week and day
    var isoRegex = /^(-|\+)?P(?:([-+]?[0-9,.]*)Y)?(?:([-+]?[0-9,.]*)M)?(?:([-+]?[0-9,.]*)W)?(?:([-+]?[0-9,.]*)D)?(?:T(?:([-+]?[0-9,.]*)H)?(?:([-+]?[0-9,.]*)M)?(?:([-+]?[0-9,.]*)S)?)?$/;

    function createDuration (input, key) {
        var duration = input,
            // matching against regexp is expensive, do it on demand
            match = null,
            sign,
            ret,
            diffRes;

        if (isDuration(input)) {
            duration = {
                ms : input._milliseconds,
                d  : input._days,
                M  : input._months
            };
        } else if (isNumber(input)) {
            duration = {};
            if (key) {
                duration[key] = input;
            } else {
                duration.milliseconds = input;
            }
        } else if (!!(match = aspNetRegex.exec(input))) {
            sign = (match[1] === '-') ? -1 : 1;
            duration = {
                y  : 0,
                d  : toInt(match[DATE])                         * sign,
                h  : toInt(match[HOUR])                         * sign,
                m  : toInt(match[MINUTE])                       * sign,
                s  : toInt(match[SECOND])                       * sign,
                ms : toInt(absRound(match[MILLISECOND] * 1000)) * sign // the millisecond decimal point is included in the match
            };
        } else if (!!(match = isoRegex.exec(input))) {
            sign = (match[1] === '-') ? -1 : 1;
            duration = {
                y : parseIso(match[2], sign),
                M : parseIso(match[3], sign),
                w : parseIso(match[4], sign),
                d : parseIso(match[5], sign),
                h : parseIso(match[6], sign),
                m : parseIso(match[7], sign),
                s : parseIso(match[8], sign)
            };
        } else if (duration == null) {// checks for null or undefined
            duration = {};
        } else if (typeof duration === 'object' && ('from' in duration || 'to' in duration)) {
            diffRes = momentsDifference(createLocal(duration.from), createLocal(duration.to));

            duration = {};
            duration.ms = diffRes.milliseconds;
            duration.M = diffRes.months;
        }

        ret = new Duration(duration);

        if (isDuration(input) && hasOwnProp(input, '_locale')) {
            ret._locale = input._locale;
        }

        return ret;
    }

    createDuration.fn = Duration.prototype;
    createDuration.invalid = createInvalid$1;

    function parseIso (inp, sign) {
        // We'd normally use ~~inp for this, but unfortunately it also
        // converts floats to ints.
        // inp may be undefined, so careful calling replace on it.
        var res = inp && parseFloat(inp.replace(',', '.'));
        // apply sign while we're at it
        return (isNaN(res) ? 0 : res) * sign;
    }

    function positiveMomentsDifference(base, other) {
        var res = {};

        res.months = other.month() - base.month() +
            (other.year() - base.year()) * 12;
        if (base.clone().add(res.months, 'M').isAfter(other)) {
            --res.months;
        }

        res.milliseconds = +other - +(base.clone().add(res.months, 'M'));

        return res;
    }

    function momentsDifference(base, other) {
        var res;
        if (!(base.isValid() && other.isValid())) {
            return {milliseconds: 0, months: 0};
        }

        other = cloneWithOffset(other, base);
        if (base.isBefore(other)) {
            res = positiveMomentsDifference(base, other);
        } else {
            res = positiveMomentsDifference(other, base);
            res.milliseconds = -res.milliseconds;
            res.months = -res.months;
        }

        return res;
    }

    // TODO: remove 'name' arg after deprecation is removed
    function createAdder(direction, name) {
        return function (val, period) {
            var dur, tmp;
            //invert the arguments, but complain about it
            if (period !== null && !isNaN(+period)) {
                deprecateSimple(name, 'moment().' + name  + '(period, number) is deprecated. Please use moment().' + name + '(number, period). ' +
                'See http://momentjs.com/guides/#/warnings/add-inverted-param/ for more info.');
                tmp = val; val = period; period = tmp;
            }

            val = typeof val === 'string' ? +val : val;
            dur = createDuration(val, period);
            addSubtract(this, dur, direction);
            return this;
        };
    }

    function addSubtract (mom, duration, isAdding, updateOffset) {
        var milliseconds = duration._milliseconds,
            days = absRound(duration._days),
            months = absRound(duration._months);

        if (!mom.isValid()) {
            // No op
            return;
        }

        updateOffset = updateOffset == null ? true : updateOffset;

        if (months) {
            setMonth(mom, get(mom, 'Month') + months * isAdding);
        }
        if (days) {
            set$1(mom, 'Date', get(mom, 'Date') + days * isAdding);
        }
        if (milliseconds) {
            mom._d.setTime(mom._d.valueOf() + milliseconds * isAdding);
        }
        if (updateOffset) {
            hooks.updateOffset(mom, days || months);
        }
    }

    var add      = createAdder(1, 'add');
    var subtract = createAdder(-1, 'subtract');

    function getCalendarFormat(myMoment, now) {
        var diff = myMoment.diff(now, 'days', true);
        return diff < -6 ? 'sameElse' :
                diff < -1 ? 'lastWeek' :
                diff < 0 ? 'lastDay' :
                diff < 1 ? 'sameDay' :
                diff < 2 ? 'nextDay' :
                diff < 7 ? 'nextWeek' : 'sameElse';
    }

    function calendar$1 (time, formats) {
        // We want to compare the start of today, vs this.
        // Getting start-of-today depends on whether we're local/utc/offset or not.
        var now = time || createLocal(),
            sod = cloneWithOffset(now, this).startOf('day'),
            format = hooks.calendarFormat(this, sod) || 'sameElse';

        var output = formats && (isFunction(formats[format]) ? formats[format].call(this, now) : formats[format]);

        return this.format(output || this.localeData().calendar(format, this, createLocal(now)));
    }

    function clone () {
        return new Moment(this);
    }

    function isAfter (input, units) {
        var localInput = isMoment(input) ? input : createLocal(input);
        if (!(this.isValid() && localInput.isValid())) {
            return false;
        }
        units = normalizeUnits(units) || 'millisecond';
        if (units === 'millisecond') {
            return this.valueOf() > localInput.valueOf();
        } else {
            return localInput.valueOf() < this.clone().startOf(units).valueOf();
        }
    }

    function isBefore (input, units) {
        var localInput = isMoment(input) ? input : createLocal(input);
        if (!(this.isValid() && localInput.isValid())) {
            return false;
        }
        units = normalizeUnits(units) || 'millisecond';
        if (units === 'millisecond') {
            return this.valueOf() < localInput.valueOf();
        } else {
            return this.clone().endOf(units).valueOf() < localInput.valueOf();
        }
    }

    function isBetween (from, to, units, inclusivity) {
        var localFrom = isMoment(from) ? from : createLocal(from),
            localTo = isMoment(to) ? to : createLocal(to);
        if (!(this.isValid() && localFrom.isValid() && localTo.isValid())) {
            return false;
        }
        inclusivity = inclusivity || '()';
        return (inclusivity[0] === '(' ? this.isAfter(localFrom, units) : !this.isBefore(localFrom, units)) &&
            (inclusivity[1] === ')' ? this.isBefore(localTo, units) : !this.isAfter(localTo, units));
    }

    function isSame (input, units) {
        var localInput = isMoment(input) ? input : createLocal(input),
            inputMs;
        if (!(this.isValid() && localInput.isValid())) {
            return false;
        }
        units = normalizeUnits(units) || 'millisecond';
        if (units === 'millisecond') {
            return this.valueOf() === localInput.valueOf();
        } else {
            inputMs = localInput.valueOf();
            return this.clone().startOf(units).valueOf() <= inputMs && inputMs <= this.clone().endOf(units).valueOf();
        }
    }

    function isSameOrAfter (input, units) {
        return this.isSame(input, units) || this.isAfter(input, units);
    }

    function isSameOrBefore (input, units) {
        return this.isSame(input, units) || this.isBefore(input, units);
    }

    function diff (input, units, asFloat) {
        var that,
            zoneDelta,
            output;

        if (!this.isValid()) {
            return NaN;
        }

        that = cloneWithOffset(input, this);

        if (!that.isValid()) {
            return NaN;
        }

        zoneDelta = (that.utcOffset() - this.utcOffset()) * 6e4;

        units = normalizeUnits(units);

        switch (units) {
            case 'year': output = monthDiff(this, that) / 12; break;
            case 'month': output = monthDiff(this, that); break;
            case 'quarter': output = monthDiff(this, that) / 3; break;
            case 'second': output = (this - that) / 1e3; break; // 1000
            case 'minute': output = (this - that) / 6e4; break; // 1000 * 60
            case 'hour': output = (this - that) / 36e5; break; // 1000 * 60 * 60
            case 'day': output = (this - that - zoneDelta) / 864e5; break; // 1000 * 60 * 60 * 24, negate dst
            case 'week': output = (this - that - zoneDelta) / 6048e5; break; // 1000 * 60 * 60 * 24 * 7, negate dst
            default: output = this - that;
        }

        return asFloat ? output : absFloor(output);
    }

    function monthDiff (a, b) {
        // difference in months
        var wholeMonthDiff = ((b.year() - a.year()) * 12) + (b.month() - a.month()),
            // b is in (anchor - 1 month, anchor + 1 month)
            anchor = a.clone().add(wholeMonthDiff, 'months'),
            anchor2, adjust;

        if (b - anchor < 0) {
            anchor2 = a.clone().add(wholeMonthDiff - 1, 'months');
            // linear across the month
            adjust = (b - anchor) / (anchor - anchor2);
        } else {
            anchor2 = a.clone().add(wholeMonthDiff + 1, 'months');
            // linear across the month
            adjust = (b - anchor) / (anchor2 - anchor);
        }

        //check for negative zero, return zero if negative zero
        return -(wholeMonthDiff + adjust) || 0;
    }

    hooks.defaultFormat = 'YYYY-MM-DDTHH:mm:ssZ';
    hooks.defaultFormatUtc = 'YYYY-MM-DDTHH:mm:ss[Z]';

    function toString () {
        return this.clone().locale('en').format('ddd MMM DD YYYY HH:mm:ss [GMT]ZZ');
    }

    function toISOString(keepOffset) {
        if (!this.isValid()) {
            return null;
        }
        var utc = keepOffset !== true;
        var m = utc ? this.clone().utc() : this;
        if (m.year() < 0 || m.year() > 9999) {
            return formatMoment(m, utc ? 'YYYYYY-MM-DD[T]HH:mm:ss.SSS[Z]' : 'YYYYYY-MM-DD[T]HH:mm:ss.SSSZ');
        }
        if (isFunction(Date.prototype.toISOString)) {
            // native implementation is ~50x faster, use it when we can
            if (utc) {
                return this.toDate().toISOString();
            } else {
                return new Date(this.valueOf() + this.utcOffset() * 60 * 1000).toISOString().replace('Z', formatMoment(m, 'Z'));
            }
        }
        return formatMoment(m, utc ? 'YYYY-MM-DD[T]HH:mm:ss.SSS[Z]' : 'YYYY-MM-DD[T]HH:mm:ss.SSSZ');
    }

    /**
     * Return a human readable representation of a moment that can
     * also be evaluated to get a new moment which is the same
     *
     * @link https://nodejs.org/dist/latest/docs/api/util.html#util_custom_inspect_function_on_objects
     */
    function inspect () {
        if (!this.isValid()) {
            return 'moment.invalid(/* ' + this._i + ' */)';
        }
        var func = 'moment';
        var zone = '';
        if (!this.isLocal()) {
            func = this.utcOffset() === 0 ? 'moment.utc' : 'moment.parseZone';
            zone = 'Z';
        }
        var prefix = '[' + func + '("]';
        var year = (0 <= this.year() && this.year() <= 9999) ? 'YYYY' : 'YYYYYY';
        var datetime = '-MM-DD[T]HH:mm:ss.SSS';
        var suffix = zone + '[")]';

        return this.format(prefix + year + datetime + suffix);
    }

    function format (inputString) {
        if (!inputString) {
            inputString = this.isUtc() ? hooks.defaultFormatUtc : hooks.defaultFormat;
        }
        var output = formatMoment(this, inputString);
        return this.localeData().postformat(output);
    }

    function from (time, withoutSuffix) {
        if (this.isValid() &&
                ((isMoment(time) && time.isValid()) ||
                 createLocal(time).isValid())) {
            return createDuration({to: this, from: time}).locale(this.locale()).humanize(!withoutSuffix);
        } else {
            return this.localeData().invalidDate();
        }
    }

    function fromNow (withoutSuffix) {
        return this.from(createLocal(), withoutSuffix);
    }

    function to (time, withoutSuffix) {
        if (this.isValid() &&
                ((isMoment(time) && time.isValid()) ||
                 createLocal(time).isValid())) {
            return createDuration({from: this, to: time}).locale(this.locale()).humanize(!withoutSuffix);
        } else {
            return this.localeData().invalidDate();
        }
    }

    function toNow (withoutSuffix) {
        return this.to(createLocal(), withoutSuffix);
    }

    // If passed a locale key, it will set the locale for this
    // instance.  Otherwise, it will return the locale configuration
    // variables for this instance.
    function locale (key) {
        var newLocaleData;

        if (key === undefined) {
            return this._locale._abbr;
        } else {
            newLocaleData = getLocale(key);
            if (newLocaleData != null) {
                this._locale = newLocaleData;
            }
            return this;
        }
    }

    var lang = deprecate(
        'moment().lang() is deprecated. Instead, use moment().localeData() to get the language configuration. Use moment().locale() to change languages.',
        function (key) {
            if (key === undefined) {
                return this.localeData();
            } else {
                return this.locale(key);
            }
        }
    );

    function localeData () {
        return this._locale;
    }

    var MS_PER_SECOND = 1000;
    var MS_PER_MINUTE = 60 * MS_PER_SECOND;
    var MS_PER_HOUR = 60 * MS_PER_MINUTE;
    var MS_PER_400_YEARS = (365 * 400 + 97) * 24 * MS_PER_HOUR;

    // actual modulo - handles negative numbers (for dates before 1970):
    function mod$1(dividend, divisor) {
        return (dividend % divisor + divisor) % divisor;
    }

    function localStartOfDate(y, m, d) {
        // the date constructor remaps years 0-99 to 1900-1999
        if (y < 100 && y >= 0) {
            // preserve leap years using a full 400 year cycle, then reset
            return new Date(y + 400, m, d) - MS_PER_400_YEARS;
        } else {
            return new Date(y, m, d).valueOf();
        }
    }

    function utcStartOfDate(y, m, d) {
        // Date.UTC remaps years 0-99 to 1900-1999
        if (y < 100 && y >= 0) {
            // preserve leap years using a full 400 year cycle, then reset
            return Date.UTC(y + 400, m, d) - MS_PER_400_YEARS;
        } else {
            return Date.UTC(y, m, d);
        }
    }

    function startOf (units) {
        var time;
        units = normalizeUnits(units);
        if (units === undefined || units === 'millisecond' || !this.isValid()) {
            return this;
        }

        var startOfDate = this._isUTC ? utcStartOfDate : localStartOfDate;

        switch (units) {
            case 'year':
                time = startOfDate(this.year(), 0, 1);
                break;
            case 'quarter':
                time = startOfDate(this.year(), this.month() - this.month() % 3, 1);
                break;
            case 'month':
                time = startOfDate(this.year(), this.month(), 1);
                break;
            case 'week':
                time = startOfDate(this.year(), this.month(), this.date() - this.weekday());
                break;
            case 'isoWeek':
                time = startOfDate(this.year(), this.month(), this.date() - (this.isoWeekday() - 1));
                break;
            case 'day':
            case 'date':
                time = startOfDate(this.year(), this.month(), this.date());
                break;
            case 'hour':
                time = this._d.valueOf();
                time -= mod$1(time + (this._isUTC ? 0 : this.utcOffset() * MS_PER_MINUTE), MS_PER_HOUR);
                break;
            case 'minute':
                time = this._d.valueOf();
                time -= mod$1(time, MS_PER_MINUTE);
                break;
            case 'second':
                time = this._d.valueOf();
                time -= mod$1(time, MS_PER_SECOND);
                break;
        }

        this._d.setTime(time);
        hooks.updateOffset(this, true);
        return this;
    }

    function endOf (units) {
        var time;
        units = normalizeUnits(units);
        if (units === undefined || units === 'millisecond' || !this.isValid()) {
            return this;
        }

        var startOfDate = this._isUTC ? utcStartOfDate : localStartOfDate;

        switch (units) {
            case 'year':
                time = startOfDate(this.year() + 1, 0, 1) - 1;
                break;
            case 'quarter':
                time = startOfDate(this.year(), this.month() - this.month() % 3 + 3, 1) - 1;
                break;
            case 'month':
                time = startOfDate(this.year(), this.month() + 1, 1) - 1;
                break;
            case 'week':
                time = startOfDate(this.year(), this.month(), this.date() - this.weekday() + 7) - 1;
                break;
            case 'isoWeek':
                time = startOfDate(this.year(), this.month(), this.date() - (this.isoWeekday() - 1) + 7) - 1;
                break;
            case 'day':
            case 'date':
                time = startOfDate(this.year(), this.month(), this.date() + 1) - 1;
                break;
            case 'hour':
                time = this._d.valueOf();
                time += MS_PER_HOUR - mod$1(time + (this._isUTC ? 0 : this.utcOffset() * MS_PER_MINUTE), MS_PER_HOUR) - 1;
                break;
            case 'minute':
                time = this._d.valueOf();
                time += MS_PER_MINUTE - mod$1(time, MS_PER_MINUTE) - 1;
                break;
            case 'second':
                time = this._d.valueOf();
                time += MS_PER_SECOND - mod$1(time, MS_PER_SECOND) - 1;
                break;
        }

        this._d.setTime(time);
        hooks.updateOffset(this, true);
        return this;
    }

    function valueOf () {
        return this._d.valueOf() - ((this._offset || 0) * 60000);
    }

    function unix () {
        return Math.floor(this.valueOf() / 1000);
    }

    function toDate () {
        return new Date(this.valueOf());
    }

    function toArray () {
        var m = this;
        return [m.year(), m.month(), m.date(), m.hour(), m.minute(), m.second(), m.millisecond()];
    }

    function toObject () {
        var m = this;
        return {
            years: m.year(),
            months: m.month(),
            date: m.date(),
            hours: m.hours(),
            minutes: m.minutes(),
            seconds: m.seconds(),
            milliseconds: m.milliseconds()
        };
    }

    function toJSON () {
        // new Date(NaN).toJSON() === null
        return this.isValid() ? this.toISOString() : null;
    }

    function isValid$2 () {
        return isValid(this);
    }

    function parsingFlags () {
        return extend({}, getParsingFlags(this));
    }

    function invalidAt () {
        return getParsingFlags(this).overflow;
    }

    function creationData() {
        return {
            input: this._i,
            format: this._f,
            locale: this._locale,
            isUTC: this._isUTC,
            strict: this._strict
        };
    }

    // FORMATTING

    addFormatToken(0, ['gg', 2], 0, function () {
        return this.weekYear() % 100;
    });

    addFormatToken(0, ['GG', 2], 0, function () {
        return this.isoWeekYear() % 100;
    });

    function addWeekYearFormatToken (token, getter) {
        addFormatToken(0, [token, token.length], 0, getter);
    }

    addWeekYearFormatToken('gggg',     'weekYear');
    addWeekYearFormatToken('ggggg',    'weekYear');
    addWeekYearFormatToken('GGGG',  'isoWeekYear');
    addWeekYearFormatToken('GGGGG', 'isoWeekYear');

    // ALIASES

    addUnitAlias('weekYear', 'gg');
    addUnitAlias('isoWeekYear', 'GG');

    // PRIORITY

    addUnitPriority('weekYear', 1);
    addUnitPriority('isoWeekYear', 1);


    // PARSING

    addRegexToken('G',      matchSigned);
    addRegexToken('g',      matchSigned);
    addRegexToken('GG',     match1to2, match2);
    addRegexToken('gg',     match1to2, match2);
    addRegexToken('GGGG',   match1to4, match4);
    addRegexToken('gggg',   match1to4, match4);
    addRegexToken('GGGGG',  match1to6, match6);
    addRegexToken('ggggg',  match1to6, match6);

    addWeekParseToken(['gggg', 'ggggg', 'GGGG', 'GGGGG'], function (input, week, config, token) {
        week[token.substr(0, 2)] = toInt(input);
    });

    addWeekParseToken(['gg', 'GG'], function (input, week, config, token) {
        week[token] = hooks.parseTwoDigitYear(input);
    });

    // MOMENTS

    function getSetWeekYear (input) {
        return getSetWeekYearHelper.call(this,
                input,
                this.week(),
                this.weekday(),
                this.localeData()._week.dow,
                this.localeData()._week.doy);
    }

    function getSetISOWeekYear (input) {
        return getSetWeekYearHelper.call(this,
                input, this.isoWeek(), this.isoWeekday(), 1, 4);
    }

    function getISOWeeksInYear () {
        return weeksInYear(this.year(), 1, 4);
    }

    function getWeeksInYear () {
        var weekInfo = this.localeData()._week;
        return weeksInYear(this.year(), weekInfo.dow, weekInfo.doy);
    }

    function getSetWeekYearHelper(input, week, weekday, dow, doy) {
        var weeksTarget;
        if (input == null) {
            return weekOfYear(this, dow, doy).year;
        } else {
            weeksTarget = weeksInYear(input, dow, doy);
            if (week > weeksTarget) {
                week = weeksTarget;
            }
            return setWeekAll.call(this, input, week, weekday, dow, doy);
        }
    }

    function setWeekAll(weekYear, week, weekday, dow, doy) {
        var dayOfYearData = dayOfYearFromWeeks(weekYear, week, weekday, dow, doy),
            date = createUTCDate(dayOfYearData.year, 0, dayOfYearData.dayOfYear);

        this.year(date.getUTCFullYear());
        this.month(date.getUTCMonth());
        this.date(date.getUTCDate());
        return this;
    }

    // FORMATTING

    addFormatToken('Q', 0, 'Qo', 'quarter');

    // ALIASES

    addUnitAlias('quarter', 'Q');

    // PRIORITY

    addUnitPriority('quarter', 7);

    // PARSING

    addRegexToken('Q', match1);
    addParseToken('Q', function (input, array) {
        array[MONTH] = (toInt(input) - 1) * 3;
    });

    // MOMENTS

    function getSetQuarter (input) {
        return input == null ? Math.ceil((this.month() + 1) / 3) : this.month((input - 1) * 3 + this.month() % 3);
    }

    // FORMATTING

    addFormatToken('D', ['DD', 2], 'Do', 'date');

    // ALIASES

    addUnitAlias('date', 'D');

    // PRIORITY
    addUnitPriority('date', 9);

    // PARSING

    addRegexToken('D',  match1to2);
    addRegexToken('DD', match1to2, match2);
    addRegexToken('Do', function (isStrict, locale) {
        // TODO: Remove "ordinalParse" fallback in next major release.
        return isStrict ?
          (locale._dayOfMonthOrdinalParse || locale._ordinalParse) :
          locale._dayOfMonthOrdinalParseLenient;
    });

    addParseToken(['D', 'DD'], DATE);
    addParseToken('Do', function (input, array) {
        array[DATE] = toInt(input.match(match1to2)[0]);
    });

    // MOMENTS

    var getSetDayOfMonth = makeGetSet('Date', true);

    // FORMATTING

    addFormatToken('DDD', ['DDDD', 3], 'DDDo', 'dayOfYear');

    // ALIASES

    addUnitAlias('dayOfYear', 'DDD');

    // PRIORITY
    addUnitPriority('dayOfYear', 4);

    // PARSING

    addRegexToken('DDD',  match1to3);
    addRegexToken('DDDD', match3);
    addParseToken(['DDD', 'DDDD'], function (input, array, config) {
        config._dayOfYear = toInt(input);
    });

    // HELPERS

    // MOMENTS

    function getSetDayOfYear (input) {
        var dayOfYear = Math.round((this.clone().startOf('day') - this.clone().startOf('year')) / 864e5) + 1;
        return input == null ? dayOfYear : this.add((input - dayOfYear), 'd');
    }

    // FORMATTING

    addFormatToken('m', ['mm', 2], 0, 'minute');

    // ALIASES

    addUnitAlias('minute', 'm');

    // PRIORITY

    addUnitPriority('minute', 14);

    // PARSING

    addRegexToken('m',  match1to2);
    addRegexToken('mm', match1to2, match2);
    addParseToken(['m', 'mm'], MINUTE);

    // MOMENTS

    var getSetMinute = makeGetSet('Minutes', false);

    // FORMATTING

    addFormatToken('s', ['ss', 2], 0, 'second');

    // ALIASES

    addUnitAlias('second', 's');

    // PRIORITY

    addUnitPriority('second', 15);

    // PARSING

    addRegexToken('s',  match1to2);
    addRegexToken('ss', match1to2, match2);
    addParseToken(['s', 'ss'], SECOND);

    // MOMENTS

    var getSetSecond = makeGetSet('Seconds', false);

    // FORMATTING

    addFormatToken('S', 0, 0, function () {
        return ~~(this.millisecond() / 100);
    });

    addFormatToken(0, ['SS', 2], 0, function () {
        return ~~(this.millisecond() / 10);
    });

    addFormatToken(0, ['SSS', 3], 0, 'millisecond');
    addFormatToken(0, ['SSSS', 4], 0, function () {
        return this.millisecond() * 10;
    });
    addFormatToken(0, ['SSSSS', 5], 0, function () {
        return this.millisecond() * 100;
    });
    addFormatToken(0, ['SSSSSS', 6], 0, function () {
        return this.millisecond() * 1000;
    });
    addFormatToken(0, ['SSSSSSS', 7], 0, function () {
        return this.millisecond() * 10000;
    });
    addFormatToken(0, ['SSSSSSSS', 8], 0, function () {
        return this.millisecond() * 100000;
    });
    addFormatToken(0, ['SSSSSSSSS', 9], 0, function () {
        return this.millisecond() * 1000000;
    });


    // ALIASES

    addUnitAlias('millisecond', 'ms');

    // PRIORITY

    addUnitPriority('millisecond', 16);

    // PARSING

    addRegexToken('S',    match1to3, match1);
    addRegexToken('SS',   match1to3, match2);
    addRegexToken('SSS',  match1to3, match3);

    var token;
    for (token = 'SSSS'; token.length <= 9; token += 'S') {
        addRegexToken(token, matchUnsigned);
    }

    function parseMs(input, array) {
        array[MILLISECOND] = toInt(('0.' + input) * 1000);
    }

    for (token = 'S'; token.length <= 9; token += 'S') {
        addParseToken(token, parseMs);
    }
    // MOMENTS

    var getSetMillisecond = makeGetSet('Milliseconds', false);

    // FORMATTING

    addFormatToken('z',  0, 0, 'zoneAbbr');
    addFormatToken('zz', 0, 0, 'zoneName');

    // MOMENTS

    function getZoneAbbr () {
        return this._isUTC ? 'UTC' : '';
    }

    function getZoneName () {
        return this._isUTC ? 'Coordinated Universal Time' : '';
    }

    var proto = Moment.prototype;

    proto.add               = add;
    proto.calendar          = calendar$1;
    proto.clone             = clone;
    proto.diff              = diff;
    proto.endOf             = endOf;
    proto.format            = format;
    proto.from              = from;
    proto.fromNow           = fromNow;
    proto.to                = to;
    proto.toNow             = toNow;
    proto.get               = stringGet;
    proto.invalidAt         = invalidAt;
    proto.isAfter           = isAfter;
    proto.isBefore          = isBefore;
    proto.isBetween         = isBetween;
    proto.isSame            = isSame;
    proto.isSameOrAfter     = isSameOrAfter;
    proto.isSameOrBefore    = isSameOrBefore;
    proto.isValid           = isValid$2;
    proto.lang              = lang;
    proto.locale            = locale;
    proto.localeData        = localeData;
    proto.max               = prototypeMax;
    proto.min               = prototypeMin;
    proto.parsingFlags      = parsingFlags;
    proto.set               = stringSet;
    proto.startOf           = startOf;
    proto.subtract          = subtract;
    proto.toArray           = toArray;
    proto.toObject          = toObject;
    proto.toDate            = toDate;
    proto.toISOString       = toISOString;
    proto.inspect           = inspect;
    proto.toJSON            = toJSON;
    proto.toString          = toString;
    proto.unix              = unix;
    proto.valueOf           = valueOf;
    proto.creationData      = creationData;
    proto.year       = getSetYear;
    proto.isLeapYear = getIsLeapYear;
    proto.weekYear    = getSetWeekYear;
    proto.isoWeekYear = getSetISOWeekYear;
    proto.quarter = proto.quarters = getSetQuarter;
    proto.month       = getSetMonth;
    proto.daysInMonth = getDaysInMonth;
    proto.week           = proto.weeks        = getSetWeek;
    proto.isoWeek        = proto.isoWeeks     = getSetISOWeek;
    proto.weeksInYear    = getWeeksInYear;
    proto.isoWeeksInYear = getISOWeeksInYear;
    proto.date       = getSetDayOfMonth;
    proto.day        = proto.days             = getSetDayOfWeek;
    proto.weekday    = getSetLocaleDayOfWeek;
    proto.isoWeekday = getSetISODayOfWeek;
    proto.dayOfYear  = getSetDayOfYear;
    proto.hour = proto.hours = getSetHour;
    proto.minute = proto.minutes = getSetMinute;
    proto.second = proto.seconds = getSetSecond;
    proto.millisecond = proto.milliseconds = getSetMillisecond;
    proto.utcOffset            = getSetOffset;
    proto.utc                  = setOffsetToUTC;
    proto.local                = setOffsetToLocal;
    proto.parseZone            = setOffsetToParsedOffset;
    proto.hasAlignedHourOffset = hasAlignedHourOffset;
    proto.isDST                = isDaylightSavingTime;
    proto.isLocal              = isLocal;
    proto.isUtcOffset          = isUtcOffset;
    proto.isUtc                = isUtc;
    proto.isUTC                = isUtc;
    proto.zoneAbbr = getZoneAbbr;
    proto.zoneName = getZoneName;
    proto.dates  = deprecate('dates accessor is deprecated. Use date instead.', getSetDayOfMonth);
    proto.months = deprecate('months accessor is deprecated. Use month instead', getSetMonth);
    proto.years  = deprecate('years accessor is deprecated. Use year instead', getSetYear);
    proto.zone   = deprecate('moment().zone is deprecated, use moment().utcOffset instead. http://momentjs.com/guides/#/warnings/zone/', getSetZone);
    proto.isDSTShifted = deprecate('isDSTShifted is deprecated. See http://momentjs.com/guides/#/warnings/dst-shifted/ for more information', isDaylightSavingTimeShifted);

    function createUnix (input) {
        return createLocal(input * 1000);
    }

    function createInZone () {
        return createLocal.apply(null, arguments).parseZone();
    }

    function preParsePostFormat (string) {
        return string;
    }

    var proto$1 = Locale.prototype;

    proto$1.calendar        = calendar;
    proto$1.longDateFormat  = longDateFormat;
    proto$1.invalidDate     = invalidDate;
    proto$1.ordinal         = ordinal;
    proto$1.preparse        = preParsePostFormat;
    proto$1.postformat      = preParsePostFormat;
    proto$1.relativeTime    = relativeTime;
    proto$1.pastFuture      = pastFuture;
    proto$1.set             = set;

    proto$1.months            =        localeMonths;
    proto$1.monthsShort       =        localeMonthsShort;
    proto$1.monthsParse       =        localeMonthsParse;
    proto$1.monthsRegex       = monthsRegex;
    proto$1.monthsShortRegex  = monthsShortRegex;
    proto$1.week = localeWeek;
    proto$1.firstDayOfYear = localeFirstDayOfYear;
    proto$1.firstDayOfWeek = localeFirstDayOfWeek;

    proto$1.weekdays       =        localeWeekdays;
    proto$1.weekdaysMin    =        localeWeekdaysMin;
    proto$1.weekdaysShort  =        localeWeekdaysShort;
    proto$1.weekdaysParse  =        localeWeekdaysParse;

    proto$1.weekdaysRegex       =        weekdaysRegex;
    proto$1.weekdaysShortRegex  =        weekdaysShortRegex;
    proto$1.weekdaysMinRegex    =        weekdaysMinRegex;

    proto$1.isPM = localeIsPM;
    proto$1.meridiem = localeMeridiem;

    function get$1 (format, index, field, setter) {
        var locale = getLocale();
        var utc = createUTC().set(setter, index);
        return locale[field](utc, format);
    }

    function listMonthsImpl (format, index, field) {
        if (isNumber(format)) {
            index = format;
            format = undefined;
        }

        format = format || '';

        if (index != null) {
            return get$1(format, index, field, 'month');
        }

        var i;
        var out = [];
        for (i = 0; i < 12; i++) {
            out[i] = get$1(format, i, field, 'month');
        }
        return out;
    }

    // ()
    // (5)
    // (fmt, 5)
    // (fmt)
    // (true)
    // (true, 5)
    // (true, fmt, 5)
    // (true, fmt)
    function listWeekdaysImpl (localeSorted, format, index, field) {
        if (typeof localeSorted === 'boolean') {
            if (isNumber(format)) {
                index = format;
                format = undefined;
            }

            format = format || '';
        } else {
            format = localeSorted;
            index = format;
            localeSorted = false;

            if (isNumber(format)) {
                index = format;
                format = undefined;
            }

            format = format || '';
        }

        var locale = getLocale(),
            shift = localeSorted ? locale._week.dow : 0;

        if (index != null) {
            return get$1(format, (index + shift) % 7, field, 'day');
        }

        var i;
        var out = [];
        for (i = 0; i < 7; i++) {
            out[i] = get$1(format, (i + shift) % 7, field, 'day');
        }
        return out;
    }

    function listMonths (format, index) {
        return listMonthsImpl(format, index, 'months');
    }

    function listMonthsShort (format, index) {
        return listMonthsImpl(format, index, 'monthsShort');
    }

    function listWeekdays (localeSorted, format, index) {
        return listWeekdaysImpl(localeSorted, format, index, 'weekdays');
    }

    function listWeekdaysShort (localeSorted, format, index) {
        return listWeekdaysImpl(localeSorted, format, index, 'weekdaysShort');
    }

    function listWeekdaysMin (localeSorted, format, index) {
        return listWeekdaysImpl(localeSorted, format, index, 'weekdaysMin');
    }

    getSetGlobalLocale('en', {
        dayOfMonthOrdinalParse: /\d{1,2}(th|st|nd|rd)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (toInt(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        }
    });

    // Side effect imports

    hooks.lang = deprecate('moment.lang is deprecated. Use moment.locale instead.', getSetGlobalLocale);
    hooks.langData = deprecate('moment.langData is deprecated. Use moment.localeData instead.', getLocale);

    var mathAbs = Math.abs;

    function abs () {
        var data           = this._data;

        this._milliseconds = mathAbs(this._milliseconds);
        this._days         = mathAbs(this._days);
        this._months       = mathAbs(this._months);

        data.milliseconds  = mathAbs(data.milliseconds);
        data.seconds       = mathAbs(data.seconds);
        data.minutes       = mathAbs(data.minutes);
        data.hours         = mathAbs(data.hours);
        data.months        = mathAbs(data.months);
        data.years         = mathAbs(data.years);

        return this;
    }

    function addSubtract$1 (duration, input, value, direction) {
        var other = createDuration(input, value);

        duration._milliseconds += direction * other._milliseconds;
        duration._days         += direction * other._days;
        duration._months       += direction * other._months;

        return duration._bubble();
    }

    // supports only 2.0-style add(1, 's') or add(duration)
    function add$1 (input, value) {
        return addSubtract$1(this, input, value, 1);
    }

    // supports only 2.0-style subtract(1, 's') or subtract(duration)
    function subtract$1 (input, value) {
        return addSubtract$1(this, input, value, -1);
    }

    function absCeil (number) {
        if (number < 0) {
            return Math.floor(number);
        } else {
            return Math.ceil(number);
        }
    }

    function bubble () {
        var milliseconds = this._milliseconds;
        var days         = this._days;
        var months       = this._months;
        var data         = this._data;
        var seconds, minutes, hours, years, monthsFromDays;

        // if we have a mix of positive and negative values, bubble down first
        // check: https://github.com/moment/moment/issues/2166
        if (!((milliseconds >= 0 && days >= 0 && months >= 0) ||
                (milliseconds <= 0 && days <= 0 && months <= 0))) {
            milliseconds += absCeil(monthsToDays(months) + days) * 864e5;
            days = 0;
            months = 0;
        }

        // The following code bubbles up values, see the tests for
        // examples of what that means.
        data.milliseconds = milliseconds % 1000;

        seconds           = absFloor(milliseconds / 1000);
        data.seconds      = seconds % 60;

        minutes           = absFloor(seconds / 60);
        data.minutes      = minutes % 60;

        hours             = absFloor(minutes / 60);
        data.hours        = hours % 24;

        days += absFloor(hours / 24);

        // convert days to months
        monthsFromDays = absFloor(daysToMonths(days));
        months += monthsFromDays;
        days -= absCeil(monthsToDays(monthsFromDays));

        // 12 months -> 1 year
        years = absFloor(months / 12);
        months %= 12;

        data.days   = days;
        data.months = months;
        data.years  = years;

        return this;
    }

    function daysToMonths (days) {
        // 400 years have 146097 days (taking into account leap year rules)
        // 400 years have 12 months === 4800
        return days * 4800 / 146097;
    }

    function monthsToDays (months) {
        // the reverse of daysToMonths
        return months * 146097 / 4800;
    }

    function as (units) {
        if (!this.isValid()) {
            return NaN;
        }
        var days;
        var months;
        var milliseconds = this._milliseconds;

        units = normalizeUnits(units);

        if (units === 'month' || units === 'quarter' || units === 'year') {
            days = this._days + milliseconds / 864e5;
            months = this._months + daysToMonths(days);
            switch (units) {
                case 'month':   return months;
                case 'quarter': return months / 3;
                case 'year':    return months / 12;
            }
        } else {
            // handle milliseconds separately because of floating point math errors (issue #1867)
            days = this._days + Math.round(monthsToDays(this._months));
            switch (units) {
                case 'week'   : return days / 7     + milliseconds / 6048e5;
                case 'day'    : return days         + milliseconds / 864e5;
                case 'hour'   : return days * 24    + milliseconds / 36e5;
                case 'minute' : return days * 1440  + milliseconds / 6e4;
                case 'second' : return days * 86400 + milliseconds / 1000;
                // Math.floor prevents floating point math errors here
                case 'millisecond': return Math.floor(days * 864e5) + milliseconds;
                default: throw new Error('Unknown unit ' + units);
            }
        }
    }

    // TODO: Use this.as('ms')?
    function valueOf$1 () {
        if (!this.isValid()) {
            return NaN;
        }
        return (
            this._milliseconds +
            this._days * 864e5 +
            (this._months % 12) * 2592e6 +
            toInt(this._months / 12) * 31536e6
        );
    }

    function makeAs (alias) {
        return function () {
            return this.as(alias);
        };
    }

    var asMilliseconds = makeAs('ms');
    var asSeconds      = makeAs('s');
    var asMinutes      = makeAs('m');
    var asHours        = makeAs('h');
    var asDays         = makeAs('d');
    var asWeeks        = makeAs('w');
    var asMonths       = makeAs('M');
    var asQuarters     = makeAs('Q');
    var asYears        = makeAs('y');

    function clone$1 () {
        return createDuration(this);
    }

    function get$2 (units) {
        units = normalizeUnits(units);
        return this.isValid() ? this[units + 's']() : NaN;
    }

    function makeGetter(name) {
        return function () {
            return this.isValid() ? this._data[name] : NaN;
        };
    }

    var milliseconds = makeGetter('milliseconds');
    var seconds      = makeGetter('seconds');
    var minutes      = makeGetter('minutes');
    var hours        = makeGetter('hours');
    var days         = makeGetter('days');
    var months       = makeGetter('months');
    var years        = makeGetter('years');

    function weeks () {
        return absFloor(this.days() / 7);
    }

    var round = Math.round;
    var thresholds = {
        ss: 44,         // a few seconds to seconds
        s : 45,         // seconds to minute
        m : 45,         // minutes to hour
        h : 22,         // hours to day
        d : 26,         // days to month
        M : 11          // months to year
    };

    // helper function for moment.fn.from, moment.fn.fromNow, and moment.duration.fn.humanize
    function substituteTimeAgo(string, number, withoutSuffix, isFuture, locale) {
        return locale.relativeTime(number || 1, !!withoutSuffix, string, isFuture);
    }

    function relativeTime$1 (posNegDuration, withoutSuffix, locale) {
        var duration = createDuration(posNegDuration).abs();
        var seconds  = round(duration.as('s'));
        var minutes  = round(duration.as('m'));
        var hours    = round(duration.as('h'));
        var days     = round(duration.as('d'));
        var months   = round(duration.as('M'));
        var years    = round(duration.as('y'));

        var a = seconds <= thresholds.ss && ['s', seconds]  ||
                seconds < thresholds.s   && ['ss', seconds] ||
                minutes <= 1             && ['m']           ||
                minutes < thresholds.m   && ['mm', minutes] ||
                hours   <= 1             && ['h']           ||
                hours   < thresholds.h   && ['hh', hours]   ||
                days    <= 1             && ['d']           ||
                days    < thresholds.d   && ['dd', days]    ||
                months  <= 1             && ['M']           ||
                months  < thresholds.M   && ['MM', months]  ||
                years   <= 1             && ['y']           || ['yy', years];

        a[2] = withoutSuffix;
        a[3] = +posNegDuration > 0;
        a[4] = locale;
        return substituteTimeAgo.apply(null, a);
    }

    // This function allows you to set the rounding function for relative time strings
    function getSetRelativeTimeRounding (roundingFunction) {
        if (roundingFunction === undefined) {
            return round;
        }
        if (typeof(roundingFunction) === 'function') {
            round = roundingFunction;
            return true;
        }
        return false;
    }

    // This function allows you to set a threshold for relative time strings
    function getSetRelativeTimeThreshold (threshold, limit) {
        if (thresholds[threshold] === undefined) {
            return false;
        }
        if (limit === undefined) {
            return thresholds[threshold];
        }
        thresholds[threshold] = limit;
        if (threshold === 's') {
            thresholds.ss = limit - 1;
        }
        return true;
    }

    function humanize (withSuffix) {
        if (!this.isValid()) {
            return this.localeData().invalidDate();
        }

        var locale = this.localeData();
        var output = relativeTime$1(this, !withSuffix, locale);

        if (withSuffix) {
            output = locale.pastFuture(+this, output);
        }

        return locale.postformat(output);
    }

    var abs$1 = Math.abs;

    function sign(x) {
        return ((x > 0) - (x < 0)) || +x;
    }

    function toISOString$1() {
        // for ISO strings we do not use the normal bubbling rules:
        //  * milliseconds bubble up until they become hours
        //  * days do not bubble at all
        //  * months bubble up until they become years
        // This is because there is no context-free conversion between hours and days
        // (think of clock changes)
        // and also not between days and months (28-31 days per month)
        if (!this.isValid()) {
            return this.localeData().invalidDate();
        }

        var seconds = abs$1(this._milliseconds) / 1000;
        var days         = abs$1(this._days);
        var months       = abs$1(this._months);
        var minutes, hours, years;

        // 3600 seconds -> 60 minutes -> 1 hour
        minutes           = absFloor(seconds / 60);
        hours             = absFloor(minutes / 60);
        seconds %= 60;
        minutes %= 60;

        // 12 months -> 1 year
        years  = absFloor(months / 12);
        months %= 12;


        // inspired by https://github.com/dordille/moment-isoduration/blob/master/moment.isoduration.js
        var Y = years;
        var M = months;
        var D = days;
        var h = hours;
        var m = minutes;
        var s = seconds ? seconds.toFixed(3).replace(/\.?0+$/, '') : '';
        var total = this.asSeconds();

        if (!total) {
            // this is the same as C#'s (Noda) and python (isodate)...
            // but not other JS (goog.date)
            return 'P0D';
        }

        var totalSign = total < 0 ? '-' : '';
        var ymSign = sign(this._months) !== sign(total) ? '-' : '';
        var daysSign = sign(this._days) !== sign(total) ? '-' : '';
        var hmsSign = sign(this._milliseconds) !== sign(total) ? '-' : '';

        return totalSign + 'P' +
            (Y ? ymSign + Y + 'Y' : '') +
            (M ? ymSign + M + 'M' : '') +
            (D ? daysSign + D + 'D' : '') +
            ((h || m || s) ? 'T' : '') +
            (h ? hmsSign + h + 'H' : '') +
            (m ? hmsSign + m + 'M' : '') +
            (s ? hmsSign + s + 'S' : '');
    }

    var proto$2 = Duration.prototype;

    proto$2.isValid        = isValid$1;
    proto$2.abs            = abs;
    proto$2.add            = add$1;
    proto$2.subtract       = subtract$1;
    proto$2.as             = as;
    proto$2.asMilliseconds = asMilliseconds;
    proto$2.asSeconds      = asSeconds;
    proto$2.asMinutes      = asMinutes;
    proto$2.asHours        = asHours;
    proto$2.asDays         = asDays;
    proto$2.asWeeks        = asWeeks;
    proto$2.asMonths       = asMonths;
    proto$2.asQuarters     = asQuarters;
    proto$2.asYears        = asYears;
    proto$2.valueOf        = valueOf$1;
    proto$2._bubble        = bubble;
    proto$2.clone          = clone$1;
    proto$2.get            = get$2;
    proto$2.milliseconds   = milliseconds;
    proto$2.seconds        = seconds;
    proto$2.minutes        = minutes;
    proto$2.hours          = hours;
    proto$2.days           = days;
    proto$2.weeks          = weeks;
    proto$2.months         = months;
    proto$2.years          = years;
    proto$2.humanize       = humanize;
    proto$2.toISOString    = toISOString$1;
    proto$2.toString       = toISOString$1;
    proto$2.toJSON         = toISOString$1;
    proto$2.locale         = locale;
    proto$2.localeData     = localeData;

    proto$2.toIsoString = deprecate('toIsoString() is deprecated. Please use toISOString() instead (notice the capitals)', toISOString$1);
    proto$2.lang = lang;

    // Side effect imports

    // FORMATTING

    addFormatToken('X', 0, 0, 'unix');
    addFormatToken('x', 0, 0, 'valueOf');

    // PARSING

    addRegexToken('x', matchSigned);
    addRegexToken('X', matchTimestamp);
    addParseToken('X', function (input, array, config) {
        config._d = new Date(parseFloat(input, 10) * 1000);
    });
    addParseToken('x', function (input, array, config) {
        config._d = new Date(toInt(input));
    });

    // Side effect imports

    //! moment.js

    hooks.version = '2.24.0';

    setHookCallback(createLocal);

    hooks.fn                    = proto;
    hooks.min                   = min;
    hooks.max                   = max;
    hooks.now                   = now;
    hooks.utc                   = createUTC;
    hooks.unix                  = createUnix;
    hooks.months                = listMonths;
    hooks.isDate                = isDate;
    hooks.locale                = getSetGlobalLocale;
    hooks.invalid               = createInvalid;
    hooks.duration              = createDuration;
    hooks.isMoment              = isMoment;
    hooks.weekdays              = listWeekdays;
    hooks.parseZone             = createInZone;
    hooks.localeData            = getLocale;
    hooks.isDuration            = isDuration;
    hooks.monthsShort           = listMonthsShort;
    hooks.weekdaysMin           = listWeekdaysMin;
    hooks.defineLocale          = defineLocale;
    hooks.updateLocale          = updateLocale;
    hooks.locales               = listLocales;
    hooks.weekdaysShort         = listWeekdaysShort;
    hooks.normalizeUnits        = normalizeUnits;
    hooks.relativeTimeRounding  = getSetRelativeTimeRounding;
    hooks.relativeTimeThreshold = getSetRelativeTimeThreshold;
    hooks.calendarFormat        = getCalendarFormat;
    hooks.prototype             = proto;

    // currently HTML5 input type only supports 24-hour formats
    hooks.HTML5_FMT = {
        DATETIME_LOCAL: 'YYYY-MM-DDTHH:mm',             // <input type="datetime-local" />
        DATETIME_LOCAL_SECONDS: 'YYYY-MM-DDTHH:mm:ss',  // <input type="datetime-local" step="1" />
        DATETIME_LOCAL_MS: 'YYYY-MM-DDTHH:mm:ss.SSS',   // <input type="datetime-local" step="0.001" />
        DATE: 'YYYY-MM-DD',                             // <input type="date" />
        TIME: 'HH:mm',                                  // <input type="time" />
        TIME_SECONDS: 'HH:mm:ss',                       // <input type="time" step="1" />
        TIME_MS: 'HH:mm:ss.SSS',                        // <input type="time" step="0.001" />
        WEEK: 'GGGG-[W]WW',                             // <input type="week" />
        MONTH: 'YYYY-MM'                                // <input type="month" />
    };

    //! moment.js locale configuration

    hooks.defineLocale('af', {
        months : 'Januarie_Februarie_Maart_April_Mei_Junie_Julie_Augustus_September_Oktober_November_Desember'.split('_'),
        monthsShort : 'Jan_Feb_Mrt_Apr_Mei_Jun_Jul_Aug_Sep_Okt_Nov_Des'.split('_'),
        weekdays : 'Sondag_Maandag_Dinsdag_Woensdag_Donderdag_Vrydag_Saterdag'.split('_'),
        weekdaysShort : 'Son_Maa_Din_Woe_Don_Vry_Sat'.split('_'),
        weekdaysMin : 'So_Ma_Di_Wo_Do_Vr_Sa'.split('_'),
        meridiemParse: /vm|nm/i,
        isPM : function (input) {
            return /^nm$/i.test(input);
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 12) {
                return isLower ? 'vm' : 'VM';
            } else {
                return isLower ? 'nm' : 'NM';
            }
        },
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Vandag om] LT',
            nextDay : '[Môre om] LT',
            nextWeek : 'dddd [om] LT',
            lastDay : '[Gister om] LT',
            lastWeek : '[Laas] dddd [om] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'oor %s',
            past : '%s gelede',
            s : '\'n paar sekondes',
            ss : '%d sekondes',
            m : '\'n minuut',
            mm : '%d minute',
            h : '\'n uur',
            hh : '%d ure',
            d : '\'n dag',
            dd : '%d dae',
            M : '\'n maand',
            MM : '%d maande',
            y : '\'n jaar',
            yy : '%d jaar'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(ste|de)/,
        ordinal : function (number) {
            return number + ((number === 1 || number === 8 || number >= 20) ? 'ste' : 'de'); // Thanks to Joris Röling : https://github.com/jjupiter
        },
        week : {
            dow : 1, // Maandag is die eerste dag van die week.
            doy : 4  // Die week wat die 4de Januarie bevat is die eerste week van die jaar.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ar-dz', {
        months : 'جانفي_فيفري_مارس_أفريل_ماي_جوان_جويلية_أوت_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        monthsShort : 'جانفي_فيفري_مارس_أفريل_ماي_جوان_جويلية_أوت_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        weekdays : 'الأحد_الإثنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'احد_اثنين_ثلاثاء_اربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'أح_إث_ثلا_أر_خم_جم_سب'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[اليوم على الساعة] LT',
            nextDay: '[غدا على الساعة] LT',
            nextWeek: 'dddd [على الساعة] LT',
            lastDay: '[أمس على الساعة] LT',
            lastWeek: 'dddd [على الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'في %s',
            past : 'منذ %s',
            s : 'ثوان',
            ss : '%d ثانية',
            m : 'دقيقة',
            mm : '%d دقائق',
            h : 'ساعة',
            hh : '%d ساعات',
            d : 'يوم',
            dd : '%d أيام',
            M : 'شهر',
            MM : '%d أشهر',
            y : 'سنة',
            yy : '%d سنوات'
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ar-kw', {
        months : 'يناير_فبراير_مارس_أبريل_ماي_يونيو_يوليوز_غشت_شتنبر_أكتوبر_نونبر_دجنبر'.split('_'),
        monthsShort : 'يناير_فبراير_مارس_أبريل_ماي_يونيو_يوليوز_غشت_شتنبر_أكتوبر_نونبر_دجنبر'.split('_'),
        weekdays : 'الأحد_الإتنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'احد_اتنين_ثلاثاء_اربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[اليوم على الساعة] LT',
            nextDay: '[غدا على الساعة] LT',
            nextWeek: 'dddd [على الساعة] LT',
            lastDay: '[أمس على الساعة] LT',
            lastWeek: 'dddd [على الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'في %s',
            past : 'منذ %s',
            s : 'ثوان',
            ss : '%d ثانية',
            m : 'دقيقة',
            mm : '%d دقائق',
            h : 'ساعة',
            hh : '%d ساعات',
            d : 'يوم',
            dd : '%d أيام',
            M : 'شهر',
            MM : '%d أشهر',
            y : 'سنة',
            yy : '%d سنوات'
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap = {
        '1': '1',
        '2': '2',
        '3': '3',
        '4': '4',
        '5': '5',
        '6': '6',
        '7': '7',
        '8': '8',
        '9': '9',
        '0': '0'
    }, pluralForm = function (n) {
        return n === 0 ? 0 : n === 1 ? 1 : n === 2 ? 2 : n % 100 >= 3 && n % 100 <= 10 ? 3 : n % 100 >= 11 ? 4 : 5;
    }, plurals = {
        s : ['أقل من ثانية', 'ثانية واحدة', ['ثانيتان', 'ثانيتين'], '%d ثوان', '%d ثانية', '%d ثانية'],
        m : ['أقل من دقيقة', 'دقيقة واحدة', ['دقيقتان', 'دقيقتين'], '%d دقائق', '%d دقيقة', '%d دقيقة'],
        h : ['أقل من ساعة', 'ساعة واحدة', ['ساعتان', 'ساعتين'], '%d ساعات', '%d ساعة', '%d ساعة'],
        d : ['أقل من يوم', 'يوم واحد', ['يومان', 'يومين'], '%d أيام', '%d يومًا', '%d يوم'],
        M : ['أقل من شهر', 'شهر واحد', ['شهران', 'شهرين'], '%d أشهر', '%d شهرا', '%d شهر'],
        y : ['أقل من عام', 'عام واحد', ['عامان', 'عامين'], '%d أعوام', '%d عامًا', '%d عام']
    }, pluralize = function (u) {
        return function (number, withoutSuffix, string, isFuture) {
            var f = pluralForm(number),
                str = plurals[u][pluralForm(number)];
            if (f === 2) {
                str = str[withoutSuffix ? 0 : 1];
            }
            return str.replace(/%d/i, number);
        };
    }, months$1 = [
        'يناير',
        'فبراير',
        'مارس',
        'أبريل',
        'مايو',
        'يونيو',
        'يوليو',
        'أغسطس',
        'سبتمبر',
        'أكتوبر',
        'نوفمبر',
        'ديسمبر'
    ];

    hooks.defineLocale('ar-ly', {
        months : months$1,
        monthsShort : months$1,
        weekdays : 'الأحد_الإثنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'أحد_إثنين_ثلاثاء_أربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'D/\u200FM/\u200FYYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        meridiemParse: /ص|م/,
        isPM : function (input) {
            return 'م' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ص';
            } else {
                return 'م';
            }
        },
        calendar : {
            sameDay: '[اليوم عند الساعة] LT',
            nextDay: '[غدًا عند الساعة] LT',
            nextWeek: 'dddd [عند الساعة] LT',
            lastDay: '[أمس عند الساعة] LT',
            lastWeek: 'dddd [عند الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'بعد %s',
            past : 'منذ %s',
            s : pluralize('s'),
            ss : pluralize('s'),
            m : pluralize('m'),
            mm : pluralize('m'),
            h : pluralize('h'),
            hh : pluralize('h'),
            d : pluralize('d'),
            dd : pluralize('d'),
            M : pluralize('M'),
            MM : pluralize('M'),
            y : pluralize('y'),
            yy : pluralize('y')
        },
        preparse: function (string) {
            return string.replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap[match];
            }).replace(/,/g, '،');
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ar-ma', {
        months : 'يناير_فبراير_مارس_أبريل_ماي_يونيو_يوليوز_غشت_شتنبر_أكتوبر_نونبر_دجنبر'.split('_'),
        monthsShort : 'يناير_فبراير_مارس_أبريل_ماي_يونيو_يوليوز_غشت_شتنبر_أكتوبر_نونبر_دجنبر'.split('_'),
        weekdays : 'الأحد_الإتنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'احد_اتنين_ثلاثاء_اربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[اليوم على الساعة] LT',
            nextDay: '[غدا على الساعة] LT',
            nextWeek: 'dddd [على الساعة] LT',
            lastDay: '[أمس على الساعة] LT',
            lastWeek: 'dddd [على الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'في %s',
            past : 'منذ %s',
            s : 'ثوان',
            ss : '%d ثانية',
            m : 'دقيقة',
            mm : '%d دقائق',
            h : 'ساعة',
            hh : '%d ساعات',
            d : 'يوم',
            dd : '%d أيام',
            M : 'شهر',
            MM : '%d أشهر',
            y : 'سنة',
            yy : '%d سنوات'
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$1 = {
        '1': '١',
        '2': '٢',
        '3': '٣',
        '4': '٤',
        '5': '٥',
        '6': '٦',
        '7': '٧',
        '8': '٨',
        '9': '٩',
        '0': '٠'
    }, numberMap = {
        '١': '1',
        '٢': '2',
        '٣': '3',
        '٤': '4',
        '٥': '5',
        '٦': '6',
        '٧': '7',
        '٨': '8',
        '٩': '9',
        '٠': '0'
    };

    hooks.defineLocale('ar-sa', {
        months : 'يناير_فبراير_مارس_أبريل_مايو_يونيو_يوليو_أغسطس_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        monthsShort : 'يناير_فبراير_مارس_أبريل_مايو_يونيو_يوليو_أغسطس_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        weekdays : 'الأحد_الإثنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'أحد_إثنين_ثلاثاء_أربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        meridiemParse: /ص|م/,
        isPM : function (input) {
            return 'م' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ص';
            } else {
                return 'م';
            }
        },
        calendar : {
            sameDay: '[اليوم على الساعة] LT',
            nextDay: '[غدا على الساعة] LT',
            nextWeek: 'dddd [على الساعة] LT',
            lastDay: '[أمس على الساعة] LT',
            lastWeek: 'dddd [على الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'في %s',
            past : 'منذ %s',
            s : 'ثوان',
            ss : '%d ثانية',
            m : 'دقيقة',
            mm : '%d دقائق',
            h : 'ساعة',
            hh : '%d ساعات',
            d : 'يوم',
            dd : '%d أيام',
            M : 'شهر',
            MM : '%d أشهر',
            y : 'سنة',
            yy : '%d سنوات'
        },
        preparse: function (string) {
            return string.replace(/[١٢٣٤٥٦٧٨٩٠]/g, function (match) {
                return numberMap[match];
            }).replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$1[match];
            }).replace(/,/g, '،');
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ar-tn', {
        months: 'جانفي_فيفري_مارس_أفريل_ماي_جوان_جويلية_أوت_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        monthsShort: 'جانفي_فيفري_مارس_أفريل_ماي_جوان_جويلية_أوت_سبتمبر_أكتوبر_نوفمبر_ديسمبر'.split('_'),
        weekdays: 'الأحد_الإثنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort: 'أحد_إثنين_ثلاثاء_أربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin: 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY HH:mm',
            LLLL: 'dddd D MMMM YYYY HH:mm'
        },
        calendar: {
            sameDay: '[اليوم على الساعة] LT',
            nextDay: '[غدا على الساعة] LT',
            nextWeek: 'dddd [على الساعة] LT',
            lastDay: '[أمس على الساعة] LT',
            lastWeek: 'dddd [على الساعة] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: 'في %s',
            past: 'منذ %s',
            s: 'ثوان',
            ss : '%d ثانية',
            m: 'دقيقة',
            mm: '%d دقائق',
            h: 'ساعة',
            hh: '%d ساعات',
            d: 'يوم',
            dd: '%d أيام',
            M: 'شهر',
            MM: '%d أشهر',
            y: 'سنة',
            yy: '%d سنوات'
        },
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4 // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$2 = {
        '1': '١',
        '2': '٢',
        '3': '٣',
        '4': '٤',
        '5': '٥',
        '6': '٦',
        '7': '٧',
        '8': '٨',
        '9': '٩',
        '0': '٠'
    }, numberMap$1 = {
        '١': '1',
        '٢': '2',
        '٣': '3',
        '٤': '4',
        '٥': '5',
        '٦': '6',
        '٧': '7',
        '٨': '8',
        '٩': '9',
        '٠': '0'
    }, pluralForm$1 = function (n) {
        return n === 0 ? 0 : n === 1 ? 1 : n === 2 ? 2 : n % 100 >= 3 && n % 100 <= 10 ? 3 : n % 100 >= 11 ? 4 : 5;
    }, plurals$1 = {
        s : ['أقل من ثانية', 'ثانية واحدة', ['ثانيتان', 'ثانيتين'], '%d ثوان', '%d ثانية', '%d ثانية'],
        m : ['أقل من دقيقة', 'دقيقة واحدة', ['دقيقتان', 'دقيقتين'], '%d دقائق', '%d دقيقة', '%d دقيقة'],
        h : ['أقل من ساعة', 'ساعة واحدة', ['ساعتان', 'ساعتين'], '%d ساعات', '%d ساعة', '%d ساعة'],
        d : ['أقل من يوم', 'يوم واحد', ['يومان', 'يومين'], '%d أيام', '%d يومًا', '%d يوم'],
        M : ['أقل من شهر', 'شهر واحد', ['شهران', 'شهرين'], '%d أشهر', '%d شهرا', '%d شهر'],
        y : ['أقل من عام', 'عام واحد', ['عامان', 'عامين'], '%d أعوام', '%d عامًا', '%d عام']
    }, pluralize$1 = function (u) {
        return function (number, withoutSuffix, string, isFuture) {
            var f = pluralForm$1(number),
                str = plurals$1[u][pluralForm$1(number)];
            if (f === 2) {
                str = str[withoutSuffix ? 0 : 1];
            }
            return str.replace(/%d/i, number);
        };
    }, months$2 = [
        'يناير',
        'فبراير',
        'مارس',
        'أبريل',
        'مايو',
        'يونيو',
        'يوليو',
        'أغسطس',
        'سبتمبر',
        'أكتوبر',
        'نوفمبر',
        'ديسمبر'
    ];

    hooks.defineLocale('ar', {
        months : months$2,
        monthsShort : months$2,
        weekdays : 'الأحد_الإثنين_الثلاثاء_الأربعاء_الخميس_الجمعة_السبت'.split('_'),
        weekdaysShort : 'أحد_إثنين_ثلاثاء_أربعاء_خميس_جمعة_سبت'.split('_'),
        weekdaysMin : 'ح_ن_ث_ر_خ_ج_س'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'D/\u200FM/\u200FYYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        meridiemParse: /ص|م/,
        isPM : function (input) {
            return 'م' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ص';
            } else {
                return 'م';
            }
        },
        calendar : {
            sameDay: '[اليوم عند الساعة] LT',
            nextDay: '[غدًا عند الساعة] LT',
            nextWeek: 'dddd [عند الساعة] LT',
            lastDay: '[أمس عند الساعة] LT',
            lastWeek: 'dddd [عند الساعة] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'بعد %s',
            past : 'منذ %s',
            s : pluralize$1('s'),
            ss : pluralize$1('s'),
            m : pluralize$1('m'),
            mm : pluralize$1('m'),
            h : pluralize$1('h'),
            hh : pluralize$1('h'),
            d : pluralize$1('d'),
            dd : pluralize$1('d'),
            M : pluralize$1('M'),
            MM : pluralize$1('M'),
            y : pluralize$1('y'),
            yy : pluralize$1('y')
        },
        preparse: function (string) {
            return string.replace(/[١٢٣٤٥٦٧٨٩٠]/g, function (match) {
                return numberMap$1[match];
            }).replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$2[match];
            }).replace(/,/g, '،');
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var suffixes = {
        1: '-inci',
        5: '-inci',
        8: '-inci',
        70: '-inci',
        80: '-inci',
        2: '-nci',
        7: '-nci',
        20: '-nci',
        50: '-nci',
        3: '-üncü',
        4: '-üncü',
        100: '-üncü',
        6: '-ncı',
        9: '-uncu',
        10: '-uncu',
        30: '-uncu',
        60: '-ıncı',
        90: '-ıncı'
    };

    hooks.defineLocale('az', {
        months : 'yanvar_fevral_mart_aprel_may_iyun_iyul_avqust_sentyabr_oktyabr_noyabr_dekabr'.split('_'),
        monthsShort : 'yan_fev_mar_apr_may_iyn_iyl_avq_sen_okt_noy_dek'.split('_'),
        weekdays : 'Bazar_Bazar ertəsi_Çərşənbə axşamı_Çərşənbə_Cümə axşamı_Cümə_Şənbə'.split('_'),
        weekdaysShort : 'Baz_BzE_ÇAx_Çər_CAx_Cüm_Şən'.split('_'),
        weekdaysMin : 'Bz_BE_ÇA_Çə_CA_Cü_Şə'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[bugün saat] LT',
            nextDay : '[sabah saat] LT',
            nextWeek : '[gələn həftə] dddd [saat] LT',
            lastDay : '[dünən] LT',
            lastWeek : '[keçən həftə] dddd [saat] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s sonra',
            past : '%s əvvəl',
            s : 'birneçə saniyə',
            ss : '%d saniyə',
            m : 'bir dəqiqə',
            mm : '%d dəqiqə',
            h : 'bir saat',
            hh : '%d saat',
            d : 'bir gün',
            dd : '%d gün',
            M : 'bir ay',
            MM : '%d ay',
            y : 'bir il',
            yy : '%d il'
        },
        meridiemParse: /gecə|səhər|gündüz|axşam/,
        isPM : function (input) {
            return /^(gündüz|axşam)$/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'gecə';
            } else if (hour < 12) {
                return 'səhər';
            } else if (hour < 17) {
                return 'gündüz';
            } else {
                return 'axşam';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(ıncı|inci|nci|üncü|ncı|uncu)/,
        ordinal : function (number) {
            if (number === 0) {  // special case for zero
                return number + '-ıncı';
            }
            var a = number % 10,
                b = number % 100 - a,
                c = number >= 100 ? 100 : null;
            return number + (suffixes[a] || suffixes[b] || suffixes[c]);
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function plural(word, num) {
        var forms = word.split('_');
        return num % 10 === 1 && num % 100 !== 11 ? forms[0] : (num % 10 >= 2 && num % 10 <= 4 && (num % 100 < 10 || num % 100 >= 20) ? forms[1] : forms[2]);
    }
    function relativeTimeWithPlural(number, withoutSuffix, key) {
        var format = {
            'ss': withoutSuffix ? 'секунда_секунды_секунд' : 'секунду_секунды_секунд',
            'mm': withoutSuffix ? 'хвіліна_хвіліны_хвілін' : 'хвіліну_хвіліны_хвілін',
            'hh': withoutSuffix ? 'гадзіна_гадзіны_гадзін' : 'гадзіну_гадзіны_гадзін',
            'dd': 'дзень_дні_дзён',
            'MM': 'месяц_месяцы_месяцаў',
            'yy': 'год_гады_гадоў'
        };
        if (key === 'm') {
            return withoutSuffix ? 'хвіліна' : 'хвіліну';
        }
        else if (key === 'h') {
            return withoutSuffix ? 'гадзіна' : 'гадзіну';
        }
        else {
            return number + ' ' + plural(format[key], +number);
        }
    }

    hooks.defineLocale('be', {
        months : {
            format: 'студзеня_лютага_сакавіка_красавіка_траўня_чэрвеня_ліпеня_жніўня_верасня_кастрычніка_лістапада_снежня'.split('_'),
            standalone: 'студзень_люты_сакавік_красавік_травень_чэрвень_ліпень_жнівень_верасень_кастрычнік_лістапад_снежань'.split('_')
        },
        monthsShort : 'студ_лют_сак_крас_трав_чэрв_ліп_жнів_вер_каст_ліст_снеж'.split('_'),
        weekdays : {
            format: 'нядзелю_панядзелак_аўторак_сераду_чацвер_пятніцу_суботу'.split('_'),
            standalone: 'нядзеля_панядзелак_аўторак_серада_чацвер_пятніца_субота'.split('_'),
            isFormat: /\[ ?[Ууў] ?(?:мінулую|наступную)? ?\] ?dddd/
        },
        weekdaysShort : 'нд_пн_ат_ср_чц_пт_сб'.split('_'),
        weekdaysMin : 'нд_пн_ат_ср_чц_пт_сб'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY г.',
            LLL : 'D MMMM YYYY г., HH:mm',
            LLLL : 'dddd, D MMMM YYYY г., HH:mm'
        },
        calendar : {
            sameDay: '[Сёння ў] LT',
            nextDay: '[Заўтра ў] LT',
            lastDay: '[Учора ў] LT',
            nextWeek: function () {
                return '[У] dddd [ў] LT';
            },
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                    case 5:
                    case 6:
                        return '[У мінулую] dddd [ў] LT';
                    case 1:
                    case 2:
                    case 4:
                        return '[У мінулы] dddd [ў] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'праз %s',
            past : '%s таму',
            s : 'некалькі секунд',
            m : relativeTimeWithPlural,
            mm : relativeTimeWithPlural,
            h : relativeTimeWithPlural,
            hh : relativeTimeWithPlural,
            d : 'дзень',
            dd : relativeTimeWithPlural,
            M : 'месяц',
            MM : relativeTimeWithPlural,
            y : 'год',
            yy : relativeTimeWithPlural
        },
        meridiemParse: /ночы|раніцы|дня|вечара/,
        isPM : function (input) {
            return /^(дня|вечара)$/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'ночы';
            } else if (hour < 12) {
                return 'раніцы';
            } else if (hour < 17) {
                return 'дня';
            } else {
                return 'вечара';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(і|ы|га)/,
        ordinal: function (number, period) {
            switch (period) {
                case 'M':
                case 'd':
                case 'DDD':
                case 'w':
                case 'W':
                    return (number % 10 === 2 || number % 10 === 3) && (number % 100 !== 12 && number % 100 !== 13) ? number + '-і' : number + '-ы';
                case 'D':
                    return number + '-га';
                default:
                    return number;
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('bg', {
        months : 'януари_февруари_март_април_май_юни_юли_август_септември_октомври_ноември_декември'.split('_'),
        monthsShort : 'янр_фев_мар_апр_май_юни_юли_авг_сеп_окт_ное_дек'.split('_'),
        weekdays : 'неделя_понеделник_вторник_сряда_четвъртък_петък_събота'.split('_'),
        weekdaysShort : 'нед_пон_вто_сря_чет_пет_съб'.split('_'),
        weekdaysMin : 'нд_пн_вт_ср_чт_пт_сб'.split('_'),
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'D.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY H:mm',
            LLLL : 'dddd, D MMMM YYYY H:mm'
        },
        calendar : {
            sameDay : '[Днес в] LT',
            nextDay : '[Утре в] LT',
            nextWeek : 'dddd [в] LT',
            lastDay : '[Вчера в] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                    case 6:
                        return '[В изминалата] dddd [в] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[В изминалия] dddd [в] LT';
                }
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'след %s',
            past : 'преди %s',
            s : 'няколко секунди',
            ss : '%d секунди',
            m : 'минута',
            mm : '%d минути',
            h : 'час',
            hh : '%d часа',
            d : 'ден',
            dd : '%d дни',
            M : 'месец',
            MM : '%d месеца',
            y : 'година',
            yy : '%d години'
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(ев|ен|ти|ви|ри|ми)/,
        ordinal : function (number) {
            var lastDigit = number % 10,
                last2Digits = number % 100;
            if (number === 0) {
                return number + '-ев';
            } else if (last2Digits === 0) {
                return number + '-ен';
            } else if (last2Digits > 10 && last2Digits < 20) {
                return number + '-ти';
            } else if (lastDigit === 1) {
                return number + '-ви';
            } else if (lastDigit === 2) {
                return number + '-ри';
            } else if (lastDigit === 7 || lastDigit === 8) {
                return number + '-ми';
            } else {
                return number + '-ти';
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('bm', {
        months : 'Zanwuyekalo_Fewuruyekalo_Marisikalo_Awirilikalo_Mɛkalo_Zuwɛnkalo_Zuluyekalo_Utikalo_Sɛtanburukalo_ɔkutɔburukalo_Nowanburukalo_Desanburukalo'.split('_'),
        monthsShort : 'Zan_Few_Mar_Awi_Mɛ_Zuw_Zul_Uti_Sɛt_ɔku_Now_Des'.split('_'),
        weekdays : 'Kari_Ntɛnɛn_Tarata_Araba_Alamisa_Juma_Sibiri'.split('_'),
        weekdaysShort : 'Kar_Ntɛ_Tar_Ara_Ala_Jum_Sib'.split('_'),
        weekdaysMin : 'Ka_Nt_Ta_Ar_Al_Ju_Si'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'MMMM [tile] D [san] YYYY',
            LLL : 'MMMM [tile] D [san] YYYY [lɛrɛ] HH:mm',
            LLLL : 'dddd MMMM [tile] D [san] YYYY [lɛrɛ] HH:mm'
        },
        calendar : {
            sameDay : '[Bi lɛrɛ] LT',
            nextDay : '[Sini lɛrɛ] LT',
            nextWeek : 'dddd [don lɛrɛ] LT',
            lastDay : '[Kunu lɛrɛ] LT',
            lastWeek : 'dddd [tɛmɛnen lɛrɛ] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s kɔnɔ',
            past : 'a bɛ %s bɔ',
            s : 'sanga dama dama',
            ss : 'sekondi %d',
            m : 'miniti kelen',
            mm : 'miniti %d',
            h : 'lɛrɛ kelen',
            hh : 'lɛrɛ %d',
            d : 'tile kelen',
            dd : 'tile %d',
            M : 'kalo kelen',
            MM : 'kalo %d',
            y : 'san kelen',
            yy : 'san %d'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$3 = {
        '1': '১',
        '2': '২',
        '3': '৩',
        '4': '৪',
        '5': '৫',
        '6': '৬',
        '7': '৭',
        '8': '৮',
        '9': '৯',
        '0': '০'
    },
    numberMap$2 = {
        '১': '1',
        '২': '2',
        '৩': '3',
        '৪': '4',
        '৫': '5',
        '৬': '6',
        '৭': '7',
        '৮': '8',
        '৯': '9',
        '০': '0'
    };

    hooks.defineLocale('bn', {
        months : 'জানুয়ারী_ফেব্রুয়ারি_মার্চ_এপ্রিল_মে_জুন_জুলাই_আগস্ট_সেপ্টেম্বর_অক্টোবর_নভেম্বর_ডিসেম্বর'.split('_'),
        monthsShort : 'জানু_ফেব_মার্চ_এপ্র_মে_জুন_জুল_আগ_সেপ্ট_অক্টো_নভে_ডিসে'.split('_'),
        weekdays : 'রবিবার_সোমবার_মঙ্গলবার_বুধবার_বৃহস্পতিবার_শুক্রবার_শনিবার'.split('_'),
        weekdaysShort : 'রবি_সোম_মঙ্গল_বুধ_বৃহস্পতি_শুক্র_শনি'.split('_'),
        weekdaysMin : 'রবি_সোম_মঙ্গ_বুধ_বৃহঃ_শুক্র_শনি'.split('_'),
        longDateFormat : {
            LT : 'A h:mm সময়',
            LTS : 'A h:mm:ss সময়',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm সময়',
            LLLL : 'dddd, D MMMM YYYY, A h:mm সময়'
        },
        calendar : {
            sameDay : '[আজ] LT',
            nextDay : '[আগামীকাল] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[গতকাল] LT',
            lastWeek : '[গত] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s পরে',
            past : '%s আগে',
            s : 'কয়েক সেকেন্ড',
            ss : '%d সেকেন্ড',
            m : 'এক মিনিট',
            mm : '%d মিনিট',
            h : 'এক ঘন্টা',
            hh : '%d ঘন্টা',
            d : 'এক দিন',
            dd : '%d দিন',
            M : 'এক মাস',
            MM : '%d মাস',
            y : 'এক বছর',
            yy : '%d বছর'
        },
        preparse: function (string) {
            return string.replace(/[১২৩৪৫৬৭৮৯০]/g, function (match) {
                return numberMap$2[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$3[match];
            });
        },
        meridiemParse: /রাত|সকাল|দুপুর|বিকাল|রাত/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if ((meridiem === 'রাত' && hour >= 4) ||
                    (meridiem === 'দুপুর' && hour < 5) ||
                    meridiem === 'বিকাল') {
                return hour + 12;
            } else {
                return hour;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'রাত';
            } else if (hour < 10) {
                return 'সকাল';
            } else if (hour < 17) {
                return 'দুপুর';
            } else if (hour < 20) {
                return 'বিকাল';
            } else {
                return 'রাত';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$4 = {
        '1': '༡',
        '2': '༢',
        '3': '༣',
        '4': '༤',
        '5': '༥',
        '6': '༦',
        '7': '༧',
        '8': '༨',
        '9': '༩',
        '0': '༠'
    },
    numberMap$3 = {
        '༡': '1',
        '༢': '2',
        '༣': '3',
        '༤': '4',
        '༥': '5',
        '༦': '6',
        '༧': '7',
        '༨': '8',
        '༩': '9',
        '༠': '0'
    };

    hooks.defineLocale('bo', {
        months : 'ཟླ་བ་དང་པོ_ཟླ་བ་གཉིས་པ_ཟླ་བ་གསུམ་པ_ཟླ་བ་བཞི་པ_ཟླ་བ་ལྔ་པ_ཟླ་བ་དྲུག་པ_ཟླ་བ་བདུན་པ_ཟླ་བ་བརྒྱད་པ_ཟླ་བ་དགུ་པ_ཟླ་བ་བཅུ་པ_ཟླ་བ་བཅུ་གཅིག་པ_ཟླ་བ་བཅུ་གཉིས་པ'.split('_'),
        monthsShort : 'ཟླ་བ་དང་པོ_ཟླ་བ་གཉིས་པ_ཟླ་བ་གསུམ་པ_ཟླ་བ་བཞི་པ_ཟླ་བ་ལྔ་པ_ཟླ་བ་དྲུག་པ_ཟླ་བ་བདུན་པ_ཟླ་བ་བརྒྱད་པ_ཟླ་བ་དགུ་པ_ཟླ་བ་བཅུ་པ_ཟླ་བ་བཅུ་གཅིག་པ_ཟླ་བ་བཅུ་གཉིས་པ'.split('_'),
        weekdays : 'གཟའ་ཉི་མ་_གཟའ་ཟླ་བ་_གཟའ་མིག་དམར་_གཟའ་ལྷག་པ་_གཟའ་ཕུར་བུ_གཟའ་པ་སངས་_གཟའ་སྤེན་པ་'.split('_'),
        weekdaysShort : 'ཉི་མ་_ཟླ་བ་_མིག་དམར་_ལྷག་པ་_ཕུར་བུ_པ་སངས་_སྤེན་པ་'.split('_'),
        weekdaysMin : 'ཉི་མ་_ཟླ་བ་_མིག་དམར་_ལྷག་པ་_ཕུར་བུ_པ་སངས་_སྤེན་པ་'.split('_'),
        longDateFormat : {
            LT : 'A h:mm',
            LTS : 'A h:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm',
            LLLL : 'dddd, D MMMM YYYY, A h:mm'
        },
        calendar : {
            sameDay : '[དི་རིང] LT',
            nextDay : '[སང་ཉིན] LT',
            nextWeek : '[བདུན་ཕྲག་རྗེས་མ], LT',
            lastDay : '[ཁ་སང] LT',
            lastWeek : '[བདུན་ཕྲག་མཐའ་མ] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s ལ་',
            past : '%s སྔན་ལ',
            s : 'ལམ་སང',
            ss : '%d སྐར་ཆ།',
            m : 'སྐར་མ་གཅིག',
            mm : '%d སྐར་མ',
            h : 'ཆུ་ཚོད་གཅིག',
            hh : '%d ཆུ་ཚོད',
            d : 'ཉིན་གཅིག',
            dd : '%d ཉིན་',
            M : 'ཟླ་བ་གཅིག',
            MM : '%d ཟླ་བ',
            y : 'ལོ་གཅིག',
            yy : '%d ལོ'
        },
        preparse: function (string) {
            return string.replace(/[༡༢༣༤༥༦༧༨༩༠]/g, function (match) {
                return numberMap$3[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$4[match];
            });
        },
        meridiemParse: /མཚན་མོ|ཞོགས་ཀས|ཉིན་གུང|དགོང་དག|མཚན་མོ/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if ((meridiem === 'མཚན་མོ' && hour >= 4) ||
                    (meridiem === 'ཉིན་གུང' && hour < 5) ||
                    meridiem === 'དགོང་དག') {
                return hour + 12;
            } else {
                return hour;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'མཚན་མོ';
            } else if (hour < 10) {
                return 'ཞོགས་ཀས';
            } else if (hour < 17) {
                return 'ཉིན་གུང';
            } else if (hour < 20) {
                return 'དགོང་དག';
            } else {
                return 'མཚན་མོ';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function relativeTimeWithMutation(number, withoutSuffix, key) {
        var format = {
            'mm': 'munutenn',
            'MM': 'miz',
            'dd': 'devezh'
        };
        return number + ' ' + mutation(format[key], number);
    }
    function specialMutationForYears(number) {
        switch (lastNumber(number)) {
            case 1:
            case 3:
            case 4:
            case 5:
            case 9:
                return number + ' bloaz';
            default:
                return number + ' vloaz';
        }
    }
    function lastNumber(number) {
        if (number > 9) {
            return lastNumber(number % 10);
        }
        return number;
    }
    function mutation(text, number) {
        if (number === 2) {
            return softMutation(text);
        }
        return text;
    }
    function softMutation(text) {
        var mutationTable = {
            'm': 'v',
            'b': 'v',
            'd': 'z'
        };
        if (mutationTable[text.charAt(0)] === undefined) {
            return text;
        }
        return mutationTable[text.charAt(0)] + text.substring(1);
    }

    hooks.defineLocale('br', {
        months : 'Genver_C\'hwevrer_Meurzh_Ebrel_Mae_Mezheven_Gouere_Eost_Gwengolo_Here_Du_Kerzu'.split('_'),
        monthsShort : 'Gen_C\'hwe_Meu_Ebr_Mae_Eve_Gou_Eos_Gwe_Her_Du_Ker'.split('_'),
        weekdays : 'Sul_Lun_Meurzh_Merc\'her_Yaou_Gwener_Sadorn'.split('_'),
        weekdaysShort : 'Sul_Lun_Meu_Mer_Yao_Gwe_Sad'.split('_'),
        weekdaysMin : 'Su_Lu_Me_Mer_Ya_Gw_Sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'h[e]mm A',
            LTS : 'h[e]mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D [a viz] MMMM YYYY',
            LLL : 'D [a viz] MMMM YYYY h[e]mm A',
            LLLL : 'dddd, D [a viz] MMMM YYYY h[e]mm A'
        },
        calendar : {
            sameDay : '[Hiziv da] LT',
            nextDay : '[Warc\'hoazh da] LT',
            nextWeek : 'dddd [da] LT',
            lastDay : '[Dec\'h da] LT',
            lastWeek : 'dddd [paset da] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'a-benn %s',
            past : '%s \'zo',
            s : 'un nebeud segondennoù',
            ss : '%d eilenn',
            m : 'ur vunutenn',
            mm : relativeTimeWithMutation,
            h : 'un eur',
            hh : '%d eur',
            d : 'un devezh',
            dd : relativeTimeWithMutation,
            M : 'ur miz',
            MM : relativeTimeWithMutation,
            y : 'ur bloaz',
            yy : specialMutationForYears
        },
        dayOfMonthOrdinalParse: /\d{1,2}(añ|vet)/,
        ordinal : function (number) {
            var output = (number === 1) ? 'añ' : 'vet';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function translate(number, withoutSuffix, key) {
        var result = number + ' ';
        switch (key) {
            case 'ss':
                if (number === 1) {
                    result += 'sekunda';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'sekunde';
                } else {
                    result += 'sekundi';
                }
                return result;
            case 'm':
                return withoutSuffix ? 'jedna minuta' : 'jedne minute';
            case 'mm':
                if (number === 1) {
                    result += 'minuta';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'minute';
                } else {
                    result += 'minuta';
                }
                return result;
            case 'h':
                return withoutSuffix ? 'jedan sat' : 'jednog sata';
            case 'hh':
                if (number === 1) {
                    result += 'sat';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'sata';
                } else {
                    result += 'sati';
                }
                return result;
            case 'dd':
                if (number === 1) {
                    result += 'dan';
                } else {
                    result += 'dana';
                }
                return result;
            case 'MM':
                if (number === 1) {
                    result += 'mjesec';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'mjeseca';
                } else {
                    result += 'mjeseci';
                }
                return result;
            case 'yy':
                if (number === 1) {
                    result += 'godina';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'godine';
                } else {
                    result += 'godina';
                }
                return result;
        }
    }

    hooks.defineLocale('bs', {
        months : 'januar_februar_mart_april_maj_juni_juli_august_septembar_oktobar_novembar_decembar'.split('_'),
        monthsShort : 'jan._feb._mar._apr._maj._jun._jul._aug._sep._okt._nov._dec.'.split('_'),
        monthsParseExact: true,
        weekdays : 'nedjelja_ponedjeljak_utorak_srijeda_četvrtak_petak_subota'.split('_'),
        weekdaysShort : 'ned._pon._uto._sri._čet._pet._sub.'.split('_'),
        weekdaysMin : 'ne_po_ut_sr_če_pe_su'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd, D. MMMM YYYY H:mm'
        },
        calendar : {
            sameDay  : '[danas u] LT',
            nextDay  : '[sutra u] LT',
            nextWeek : function () {
                switch (this.day()) {
                    case 0:
                        return '[u] [nedjelju] [u] LT';
                    case 3:
                        return '[u] [srijedu] [u] LT';
                    case 6:
                        return '[u] [subotu] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[u] dddd [u] LT';
                }
            },
            lastDay  : '[jučer u] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                        return '[prošlu] dddd [u] LT';
                    case 6:
                        return '[prošle] [subote] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[prošli] dddd [u] LT';
                }
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'za %s',
            past   : 'prije %s',
            s      : 'par sekundi',
            ss     : translate,
            m      : translate,
            mm     : translate,
            h      : translate,
            hh     : translate,
            d      : 'dan',
            dd     : translate,
            M      : 'mjesec',
            MM     : translate,
            y      : 'godinu',
            yy     : translate
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ca', {
        months : {
            standalone: 'gener_febrer_març_abril_maig_juny_juliol_agost_setembre_octubre_novembre_desembre'.split('_'),
            format: 'de gener_de febrer_de març_d\'abril_de maig_de juny_de juliol_d\'agost_de setembre_d\'octubre_de novembre_de desembre'.split('_'),
            isFormat: /D[oD]?(\s)+MMMM/
        },
        monthsShort : 'gen._febr._març_abr._maig_juny_jul._ag._set._oct._nov._des.'.split('_'),
        monthsParseExact : true,
        weekdays : 'diumenge_dilluns_dimarts_dimecres_dijous_divendres_dissabte'.split('_'),
        weekdaysShort : 'dg._dl._dt._dc._dj._dv._ds.'.split('_'),
        weekdaysMin : 'dg_dl_dt_dc_dj_dv_ds'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM [de] YYYY',
            ll : 'D MMM YYYY',
            LLL : 'D MMMM [de] YYYY [a les] H:mm',
            lll : 'D MMM YYYY, H:mm',
            LLLL : 'dddd D MMMM [de] YYYY [a les] H:mm',
            llll : 'ddd D MMM YYYY, H:mm'
        },
        calendar : {
            sameDay : function () {
                return '[avui a ' + ((this.hours() !== 1) ? 'les' : 'la') + '] LT';
            },
            nextDay : function () {
                return '[demà a ' + ((this.hours() !== 1) ? 'les' : 'la') + '] LT';
            },
            nextWeek : function () {
                return 'dddd [a ' + ((this.hours() !== 1) ? 'les' : 'la') + '] LT';
            },
            lastDay : function () {
                return '[ahir a ' + ((this.hours() !== 1) ? 'les' : 'la') + '] LT';
            },
            lastWeek : function () {
                return '[el] dddd [passat a ' + ((this.hours() !== 1) ? 'les' : 'la') + '] LT';
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'd\'aquí %s',
            past : 'fa %s',
            s : 'uns segons',
            ss : '%d segons',
            m : 'un minut',
            mm : '%d minuts',
            h : 'una hora',
            hh : '%d hores',
            d : 'un dia',
            dd : '%d dies',
            M : 'un mes',
            MM : '%d mesos',
            y : 'un any',
            yy : '%d anys'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(r|n|t|è|a)/,
        ordinal : function (number, period) {
            var output = (number === 1) ? 'r' :
                (number === 2) ? 'n' :
                (number === 3) ? 'r' :
                (number === 4) ? 't' : 'è';
            if (period === 'w' || period === 'W') {
                output = 'a';
            }
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var months$3 = 'leden_únor_březen_duben_květen_červen_červenec_srpen_září_říjen_listopad_prosinec'.split('_'),
        monthsShort = 'led_úno_bře_dub_kvě_čvn_čvc_srp_zář_říj_lis_pro'.split('_');

    var monthsParse = [/^led/i, /^úno/i, /^bře/i, /^dub/i, /^kvě/i, /^(čvn|červen$|června)/i, /^(čvc|červenec|července)/i, /^srp/i, /^zář/i, /^říj/i, /^lis/i, /^pro/i];
    // NOTE: 'červen' is substring of 'červenec'; therefore 'červenec' must precede 'červen' in the regex to be fully matched.
    // Otherwise parser matches '1. červenec' as '1. červen' + 'ec'.
    var monthsRegex$1 = /^(leden|únor|březen|duben|květen|červenec|července|červen|června|srpen|září|říjen|listopad|prosinec|led|úno|bře|dub|kvě|čvn|čvc|srp|zář|říj|lis|pro)/i;

    function plural$1(n) {
        return (n > 1) && (n < 5) && (~~(n / 10) !== 1);
    }
    function translate$1(number, withoutSuffix, key, isFuture) {
        var result = number + ' ';
        switch (key) {
            case 's':  // a few seconds / in a few seconds / a few seconds ago
                return (withoutSuffix || isFuture) ? 'pár sekund' : 'pár sekundami';
            case 'ss': // 9 seconds / in 9 seconds / 9 seconds ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'sekundy' : 'sekund');
                } else {
                    return result + 'sekundami';
                }
                break;
            case 'm':  // a minute / in a minute / a minute ago
                return withoutSuffix ? 'minuta' : (isFuture ? 'minutu' : 'minutou');
            case 'mm': // 9 minutes / in 9 minutes / 9 minutes ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'minuty' : 'minut');
                } else {
                    return result + 'minutami';
                }
                break;
            case 'h':  // an hour / in an hour / an hour ago
                return withoutSuffix ? 'hodina' : (isFuture ? 'hodinu' : 'hodinou');
            case 'hh': // 9 hours / in 9 hours / 9 hours ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'hodiny' : 'hodin');
                } else {
                    return result + 'hodinami';
                }
                break;
            case 'd':  // a day / in a day / a day ago
                return (withoutSuffix || isFuture) ? 'den' : 'dnem';
            case 'dd': // 9 days / in 9 days / 9 days ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'dny' : 'dní');
                } else {
                    return result + 'dny';
                }
                break;
            case 'M':  // a month / in a month / a month ago
                return (withoutSuffix || isFuture) ? 'měsíc' : 'měsícem';
            case 'MM': // 9 months / in 9 months / 9 months ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'měsíce' : 'měsíců');
                } else {
                    return result + 'měsíci';
                }
                break;
            case 'y':  // a year / in a year / a year ago
                return (withoutSuffix || isFuture) ? 'rok' : 'rokem';
            case 'yy': // 9 years / in 9 years / 9 years ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$1(number) ? 'roky' : 'let');
                } else {
                    return result + 'lety';
                }
                break;
        }
    }

    hooks.defineLocale('cs', {
        months : months$3,
        monthsShort : monthsShort,
        monthsRegex : monthsRegex$1,
        monthsShortRegex : monthsRegex$1,
        // NOTE: 'červen' is substring of 'červenec'; therefore 'červenec' must precede 'červen' in the regex to be fully matched.
        // Otherwise parser matches '1. červenec' as '1. červen' + 'ec'.
        monthsStrictRegex : /^(leden|ledna|února|únor|březen|března|duben|dubna|květen|května|červenec|července|červen|června|srpen|srpna|září|říjen|října|listopadu|listopad|prosinec|prosince)/i,
        monthsShortStrictRegex : /^(led|úno|bře|dub|kvě|čvn|čvc|srp|zář|říj|lis|pro)/i,
        monthsParse : monthsParse,
        longMonthsParse : monthsParse,
        shortMonthsParse : monthsParse,
        weekdays : 'neděle_pondělí_úterý_středa_čtvrtek_pátek_sobota'.split('_'),
        weekdaysShort : 'ne_po_út_st_čt_pá_so'.split('_'),
        weekdaysMin : 'ne_po_út_st_čt_pá_so'.split('_'),
        longDateFormat : {
            LT: 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd D. MMMM YYYY H:mm',
            l : 'D. M. YYYY'
        },
        calendar : {
            sameDay: '[dnes v] LT',
            nextDay: '[zítra v] LT',
            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[v neděli v] LT';
                    case 1:
                    case 2:
                        return '[v] dddd [v] LT';
                    case 3:
                        return '[ve středu v] LT';
                    case 4:
                        return '[ve čtvrtek v] LT';
                    case 5:
                        return '[v pátek v] LT';
                    case 6:
                        return '[v sobotu v] LT';
                }
            },
            lastDay: '[včera v] LT',
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[minulou neděli v] LT';
                    case 1:
                    case 2:
                        return '[minulé] dddd [v] LT';
                    case 3:
                        return '[minulou středu v] LT';
                    case 4:
                    case 5:
                        return '[minulý] dddd [v] LT';
                    case 6:
                        return '[minulou sobotu v] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'za %s',
            past : 'před %s',
            s : translate$1,
            ss : translate$1,
            m : translate$1,
            mm : translate$1,
            h : translate$1,
            hh : translate$1,
            d : translate$1,
            dd : translate$1,
            M : translate$1,
            MM : translate$1,
            y : translate$1,
            yy : translate$1
        },
        dayOfMonthOrdinalParse : /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('cv', {
        months : 'кӑрлач_нарӑс_пуш_ака_май_ҫӗртме_утӑ_ҫурла_авӑн_юпа_чӳк_раштав'.split('_'),
        monthsShort : 'кӑр_нар_пуш_ака_май_ҫӗр_утӑ_ҫур_авн_юпа_чӳк_раш'.split('_'),
        weekdays : 'вырсарникун_тунтикун_ытларикун_юнкун_кӗҫнерникун_эрнекун_шӑматкун'.split('_'),
        weekdaysShort : 'выр_тун_ытл_юн_кӗҫ_эрн_шӑм'.split('_'),
        weekdaysMin : 'вр_тн_ыт_юн_кҫ_эр_шм'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD-MM-YYYY',
            LL : 'YYYY [ҫулхи] MMMM [уйӑхӗн] D[-мӗшӗ]',
            LLL : 'YYYY [ҫулхи] MMMM [уйӑхӗн] D[-мӗшӗ], HH:mm',
            LLLL : 'dddd, YYYY [ҫулхи] MMMM [уйӑхӗн] D[-мӗшӗ], HH:mm'
        },
        calendar : {
            sameDay: '[Паян] LT [сехетре]',
            nextDay: '[Ыран] LT [сехетре]',
            lastDay: '[Ӗнер] LT [сехетре]',
            nextWeek: '[Ҫитес] dddd LT [сехетре]',
            lastWeek: '[Иртнӗ] dddd LT [сехетре]',
            sameElse: 'L'
        },
        relativeTime : {
            future : function (output) {
                var affix = /сехет$/i.exec(output) ? 'рен' : /ҫул$/i.exec(output) ? 'тан' : 'ран';
                return output + affix;
            },
            past : '%s каялла',
            s : 'пӗр-ик ҫеккунт',
            ss : '%d ҫеккунт',
            m : 'пӗр минут',
            mm : '%d минут',
            h : 'пӗр сехет',
            hh : '%d сехет',
            d : 'пӗр кун',
            dd : '%d кун',
            M : 'пӗр уйӑх',
            MM : '%d уйӑх',
            y : 'пӗр ҫул',
            yy : '%d ҫул'
        },
        dayOfMonthOrdinalParse: /\d{1,2}-мӗш/,
        ordinal : '%d-мӗш',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('cy', {
        months: 'Ionawr_Chwefror_Mawrth_Ebrill_Mai_Mehefin_Gorffennaf_Awst_Medi_Hydref_Tachwedd_Rhagfyr'.split('_'),
        monthsShort: 'Ion_Chwe_Maw_Ebr_Mai_Meh_Gor_Aws_Med_Hyd_Tach_Rhag'.split('_'),
        weekdays: 'Dydd Sul_Dydd Llun_Dydd Mawrth_Dydd Mercher_Dydd Iau_Dydd Gwener_Dydd Sadwrn'.split('_'),
        weekdaysShort: 'Sul_Llun_Maw_Mer_Iau_Gwe_Sad'.split('_'),
        weekdaysMin: 'Su_Ll_Ma_Me_Ia_Gw_Sa'.split('_'),
        weekdaysParseExact : true,
        // time formats are the same as en-gb
        longDateFormat: {
            LT: 'HH:mm',
            LTS : 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY HH:mm',
            LLLL: 'dddd, D MMMM YYYY HH:mm'
        },
        calendar: {
            sameDay: '[Heddiw am] LT',
            nextDay: '[Yfory am] LT',
            nextWeek: 'dddd [am] LT',
            lastDay: '[Ddoe am] LT',
            lastWeek: 'dddd [diwethaf am] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: 'mewn %s',
            past: '%s yn ôl',
            s: 'ychydig eiliadau',
            ss: '%d eiliad',
            m: 'munud',
            mm: '%d munud',
            h: 'awr',
            hh: '%d awr',
            d: 'diwrnod',
            dd: '%d diwrnod',
            M: 'mis',
            MM: '%d mis',
            y: 'blwyddyn',
            yy: '%d flynedd'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(fed|ain|af|il|ydd|ed|eg)/,
        // traditional ordinal numbers above 31 are not commonly used in colloquial Welsh
        ordinal: function (number) {
            var b = number,
                output = '',
                lookup = [
                    '', 'af', 'il', 'ydd', 'ydd', 'ed', 'ed', 'ed', 'fed', 'fed', 'fed', // 1af to 10fed
                    'eg', 'fed', 'eg', 'eg', 'fed', 'eg', 'eg', 'fed', 'eg', 'fed' // 11eg to 20fed
                ];
            if (b > 20) {
                if (b === 40 || b === 50 || b === 60 || b === 80 || b === 100) {
                    output = 'fed'; // not 30ain, 70ain or 90ain
                } else {
                    output = 'ain';
                }
            } else if (b > 0) {
                output = lookup[b];
            }
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('da', {
        months : 'januar_februar_marts_april_maj_juni_juli_august_september_oktober_november_december'.split('_'),
        monthsShort : 'jan_feb_mar_apr_maj_jun_jul_aug_sep_okt_nov_dec'.split('_'),
        weekdays : 'søndag_mandag_tirsdag_onsdag_torsdag_fredag_lørdag'.split('_'),
        weekdaysShort : 'søn_man_tir_ons_tor_fre_lør'.split('_'),
        weekdaysMin : 'sø_ma_ti_on_to_fr_lø'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY HH:mm',
            LLLL : 'dddd [d.] D. MMMM YYYY [kl.] HH:mm'
        },
        calendar : {
            sameDay : '[i dag kl.] LT',
            nextDay : '[i morgen kl.] LT',
            nextWeek : 'på dddd [kl.] LT',
            lastDay : '[i går kl.] LT',
            lastWeek : '[i] dddd[s kl.] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'om %s',
            past : '%s siden',
            s : 'få sekunder',
            ss : '%d sekunder',
            m : 'et minut',
            mm : '%d minutter',
            h : 'en time',
            hh : '%d timer',
            d : 'en dag',
            dd : '%d dage',
            M : 'en måned',
            MM : '%d måneder',
            y : 'et år',
            yy : '%d år'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime(number, withoutSuffix, key, isFuture) {
        var format = {
            'm': ['eine Minute', 'einer Minute'],
            'h': ['eine Stunde', 'einer Stunde'],
            'd': ['ein Tag', 'einem Tag'],
            'dd': [number + ' Tage', number + ' Tagen'],
            'M': ['ein Monat', 'einem Monat'],
            'MM': [number + ' Monate', number + ' Monaten'],
            'y': ['ein Jahr', 'einem Jahr'],
            'yy': [number + ' Jahre', number + ' Jahren']
        };
        return withoutSuffix ? format[key][0] : format[key][1];
    }

    hooks.defineLocale('de-at', {
        months : 'Jänner_Februar_März_April_Mai_Juni_Juli_August_September_Oktober_November_Dezember'.split('_'),
        monthsShort : 'Jän._Feb._März_Apr._Mai_Juni_Juli_Aug._Sep._Okt._Nov._Dez.'.split('_'),
        monthsParseExact : true,
        weekdays : 'Sonntag_Montag_Dienstag_Mittwoch_Donnerstag_Freitag_Samstag'.split('_'),
        weekdaysShort : 'So._Mo._Di._Mi._Do._Fr._Sa.'.split('_'),
        weekdaysMin : 'So_Mo_Di_Mi_Do_Fr_Sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY HH:mm',
            LLLL : 'dddd, D. MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[heute um] LT [Uhr]',
            sameElse: 'L',
            nextDay: '[morgen um] LT [Uhr]',
            nextWeek: 'dddd [um] LT [Uhr]',
            lastDay: '[gestern um] LT [Uhr]',
            lastWeek: '[letzten] dddd [um] LT [Uhr]'
        },
        relativeTime : {
            future : 'in %s',
            past : 'vor %s',
            s : 'ein paar Sekunden',
            ss : '%d Sekunden',
            m : processRelativeTime,
            mm : '%d Minuten',
            h : processRelativeTime,
            hh : '%d Stunden',
            d : processRelativeTime,
            dd : processRelativeTime,
            M : processRelativeTime,
            MM : processRelativeTime,
            y : processRelativeTime,
            yy : processRelativeTime
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$1(number, withoutSuffix, key, isFuture) {
        var format = {
            'm': ['eine Minute', 'einer Minute'],
            'h': ['eine Stunde', 'einer Stunde'],
            'd': ['ein Tag', 'einem Tag'],
            'dd': [number + ' Tage', number + ' Tagen'],
            'M': ['ein Monat', 'einem Monat'],
            'MM': [number + ' Monate', number + ' Monaten'],
            'y': ['ein Jahr', 'einem Jahr'],
            'yy': [number + ' Jahre', number + ' Jahren']
        };
        return withoutSuffix ? format[key][0] : format[key][1];
    }

    hooks.defineLocale('de-ch', {
        months : 'Januar_Februar_März_April_Mai_Juni_Juli_August_September_Oktober_November_Dezember'.split('_'),
        monthsShort : 'Jan._Feb._März_Apr._Mai_Juni_Juli_Aug._Sep._Okt._Nov._Dez.'.split('_'),
        monthsParseExact : true,
        weekdays : 'Sonntag_Montag_Dienstag_Mittwoch_Donnerstag_Freitag_Samstag'.split('_'),
        weekdaysShort : 'So_Mo_Di_Mi_Do_Fr_Sa'.split('_'),
        weekdaysMin : 'So_Mo_Di_Mi_Do_Fr_Sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY HH:mm',
            LLLL : 'dddd, D. MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[heute um] LT [Uhr]',
            sameElse: 'L',
            nextDay: '[morgen um] LT [Uhr]',
            nextWeek: 'dddd [um] LT [Uhr]',
            lastDay: '[gestern um] LT [Uhr]',
            lastWeek: '[letzten] dddd [um] LT [Uhr]'
        },
        relativeTime : {
            future : 'in %s',
            past : 'vor %s',
            s : 'ein paar Sekunden',
            ss : '%d Sekunden',
            m : processRelativeTime$1,
            mm : '%d Minuten',
            h : processRelativeTime$1,
            hh : '%d Stunden',
            d : processRelativeTime$1,
            dd : processRelativeTime$1,
            M : processRelativeTime$1,
            MM : processRelativeTime$1,
            y : processRelativeTime$1,
            yy : processRelativeTime$1
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$2(number, withoutSuffix, key, isFuture) {
        var format = {
            'm': ['eine Minute', 'einer Minute'],
            'h': ['eine Stunde', 'einer Stunde'],
            'd': ['ein Tag', 'einem Tag'],
            'dd': [number + ' Tage', number + ' Tagen'],
            'M': ['ein Monat', 'einem Monat'],
            'MM': [number + ' Monate', number + ' Monaten'],
            'y': ['ein Jahr', 'einem Jahr'],
            'yy': [number + ' Jahre', number + ' Jahren']
        };
        return withoutSuffix ? format[key][0] : format[key][1];
    }

    hooks.defineLocale('de', {
        months : 'Januar_Februar_März_April_Mai_Juni_Juli_August_September_Oktober_November_Dezember'.split('_'),
        monthsShort : 'Jan._Feb._März_Apr._Mai_Juni_Juli_Aug._Sep._Okt._Nov._Dez.'.split('_'),
        monthsParseExact : true,
        weekdays : 'Sonntag_Montag_Dienstag_Mittwoch_Donnerstag_Freitag_Samstag'.split('_'),
        weekdaysShort : 'So._Mo._Di._Mi._Do._Fr._Sa.'.split('_'),
        weekdaysMin : 'So_Mo_Di_Mi_Do_Fr_Sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY HH:mm',
            LLLL : 'dddd, D. MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[heute um] LT [Uhr]',
            sameElse: 'L',
            nextDay: '[morgen um] LT [Uhr]',
            nextWeek: 'dddd [um] LT [Uhr]',
            lastDay: '[gestern um] LT [Uhr]',
            lastWeek: '[letzten] dddd [um] LT [Uhr]'
        },
        relativeTime : {
            future : 'in %s',
            past : 'vor %s',
            s : 'ein paar Sekunden',
            ss : '%d Sekunden',
            m : processRelativeTime$2,
            mm : '%d Minuten',
            h : processRelativeTime$2,
            hh : '%d Stunden',
            d : processRelativeTime$2,
            dd : processRelativeTime$2,
            M : processRelativeTime$2,
            MM : processRelativeTime$2,
            y : processRelativeTime$2,
            yy : processRelativeTime$2
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var months$4 = [
        'ޖެނުއަރީ',
        'ފެބްރުއަރީ',
        'މާރިޗު',
        'އޭޕްރީލު',
        'މޭ',
        'ޖޫން',
        'ޖުލައި',
        'އޯގަސްޓު',
        'ސެޕްޓެމްބަރު',
        'އޮކްޓޯބަރު',
        'ނޮވެމްބަރު',
        'ޑިސެމްބަރު'
    ], weekdays = [
        'އާދިއްތަ',
        'ހޯމަ',
        'އަންގާރަ',
        'ބުދަ',
        'ބުރާސްފަތި',
        'ހުކުރު',
        'ހޮނިހިރު'
    ];

    hooks.defineLocale('dv', {
        months : months$4,
        monthsShort : months$4,
        weekdays : weekdays,
        weekdaysShort : weekdays,
        weekdaysMin : 'އާދި_ހޯމަ_އަން_ބުދަ_ބުރާ_ހުކު_ހޮނި'.split('_'),
        longDateFormat : {

            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'D/M/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        meridiemParse: /މކ|މފ/,
        isPM : function (input) {
            return 'މފ' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'މކ';
            } else {
                return 'މފ';
            }
        },
        calendar : {
            sameDay : '[މިއަދު] LT',
            nextDay : '[މާދަމާ] LT',
            nextWeek : 'dddd LT',
            lastDay : '[އިއްޔެ] LT',
            lastWeek : '[ފާއިތުވި] dddd LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'ތެރޭގައި %s',
            past : 'ކުރިން %s',
            s : 'ސިކުންތުކޮޅެއް',
            ss : 'd% ސިކުންތު',
            m : 'މިނިޓެއް',
            mm : 'މިނިޓު %d',
            h : 'ގަޑިއިރެއް',
            hh : 'ގަޑިއިރު %d',
            d : 'ދުވަހެއް',
            dd : 'ދުވަސް %d',
            M : 'މަހެއް',
            MM : 'މަސް %d',
            y : 'އަހަރެއް',
            yy : 'އަހަރު %d'
        },
        preparse: function (string) {
            return string.replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/,/g, '،');
        },
        week : {
            dow : 7,  // Sunday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('el', {
        monthsNominativeEl : 'Ιανουάριος_Φεβρουάριος_Μάρτιος_Απρίλιος_Μάιος_Ιούνιος_Ιούλιος_Αύγουστος_Σεπτέμβριος_Οκτώβριος_Νοέμβριος_Δεκέμβριος'.split('_'),
        monthsGenitiveEl : 'Ιανουαρίου_Φεβρουαρίου_Μαρτίου_Απριλίου_Μαΐου_Ιουνίου_Ιουλίου_Αυγούστου_Σεπτεμβρίου_Οκτωβρίου_Νοεμβρίου_Δεκεμβρίου'.split('_'),
        months : function (momentToFormat, format) {
            if (!momentToFormat) {
                return this._monthsNominativeEl;
            } else if (typeof format === 'string' && /D/.test(format.substring(0, format.indexOf('MMMM')))) { // if there is a day number before 'MMMM'
                return this._monthsGenitiveEl[momentToFormat.month()];
            } else {
                return this._monthsNominativeEl[momentToFormat.month()];
            }
        },
        monthsShort : 'Ιαν_Φεβ_Μαρ_Απρ_Μαϊ_Ιουν_Ιουλ_Αυγ_Σεπ_Οκτ_Νοε_Δεκ'.split('_'),
        weekdays : 'Κυριακή_Δευτέρα_Τρίτη_Τετάρτη_Πέμπτη_Παρασκευή_Σάββατο'.split('_'),
        weekdaysShort : 'Κυρ_Δευ_Τρι_Τετ_Πεμ_Παρ_Σαβ'.split('_'),
        weekdaysMin : 'Κυ_Δε_Τρ_Τε_Πε_Πα_Σα'.split('_'),
        meridiem : function (hours, minutes, isLower) {
            if (hours > 11) {
                return isLower ? 'μμ' : 'ΜΜ';
            } else {
                return isLower ? 'πμ' : 'ΠΜ';
            }
        },
        isPM : function (input) {
            return ((input + '').toLowerCase()[0] === 'μ');
        },
        meridiemParse : /[ΠΜ]\.?Μ?\.?/i,
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendarEl : {
            sameDay : '[Σήμερα {}] LT',
            nextDay : '[Αύριο {}] LT',
            nextWeek : 'dddd [{}] LT',
            lastDay : '[Χθες {}] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 6:
                        return '[το προηγούμενο] dddd [{}] LT';
                    default:
                        return '[την προηγούμενη] dddd [{}] LT';
                }
            },
            sameElse : 'L'
        },
        calendar : function (key, mom) {
            var output = this._calendarEl[key],
                hours = mom && mom.hours();
            if (isFunction(output)) {
                output = output.apply(mom);
            }
            return output.replace('{}', (hours % 12 === 1 ? 'στη' : 'στις'));
        },
        relativeTime : {
            future : 'σε %s',
            past : '%s πριν',
            s : 'λίγα δευτερόλεπτα',
            ss : '%d δευτερόλεπτα',
            m : 'ένα λεπτό',
            mm : '%d λεπτά',
            h : 'μία ώρα',
            hh : '%d ώρες',
            d : 'μία μέρα',
            dd : '%d μέρες',
            M : 'ένας μήνας',
            MM : '%d μήνες',
            y : 'ένας χρόνος',
            yy : '%d χρόνια'
        },
        dayOfMonthOrdinalParse: /\d{1,2}η/,
        ordinal: '%dη',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4st is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-SG', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-au', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-ca', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'YYYY-MM-DD',
            LL : 'MMMM D, YYYY',
            LLL : 'MMMM D, YYYY h:mm A',
            LLLL : 'dddd, MMMM D, YYYY h:mm A'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-gb', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-ie', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-il', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('en-nz', {
        months : 'January_February_March_April_May_June_July_August_September_October_November_December'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_May_Jun_Jul_Aug_Sep_Oct_Nov_Dec'.split('_'),
        weekdays : 'Sunday_Monday_Tuesday_Wednesday_Thursday_Friday_Saturday'.split('_'),
        weekdaysShort : 'Sun_Mon_Tue_Wed_Thu_Fri_Sat'.split('_'),
        weekdaysMin : 'Su_Mo_Tu_We_Th_Fr_Sa'.split('_'),
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendar : {
            sameDay : '[Today at] LT',
            nextDay : '[Tomorrow at] LT',
            nextWeek : 'dddd [at] LT',
            lastDay : '[Yesterday at] LT',
            lastWeek : '[Last] dddd [at] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'in %s',
            past : '%s ago',
            s : 'a few seconds',
            ss : '%d seconds',
            m : 'a minute',
            mm : '%d minutes',
            h : 'an hour',
            hh : '%d hours',
            d : 'a day',
            dd : '%d days',
            M : 'a month',
            MM : '%d months',
            y : 'a year',
            yy : '%d years'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('eo', {
        months : 'januaro_februaro_marto_aprilo_majo_junio_julio_aŭgusto_septembro_oktobro_novembro_decembro'.split('_'),
        monthsShort : 'jan_feb_mar_apr_maj_jun_jul_aŭg_sep_okt_nov_dec'.split('_'),
        weekdays : 'dimanĉo_lundo_mardo_merkredo_ĵaŭdo_vendredo_sabato'.split('_'),
        weekdaysShort : 'dim_lun_mard_merk_ĵaŭ_ven_sab'.split('_'),
        weekdaysMin : 'di_lu_ma_me_ĵa_ve_sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'D[-a de] MMMM, YYYY',
            LLL : 'D[-a de] MMMM, YYYY HH:mm',
            LLLL : 'dddd, [la] D[-a de] MMMM, YYYY HH:mm'
        },
        meridiemParse: /[ap]\.t\.m/i,
        isPM: function (input) {
            return input.charAt(0).toLowerCase() === 'p';
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours > 11) {
                return isLower ? 'p.t.m.' : 'P.T.M.';
            } else {
                return isLower ? 'a.t.m.' : 'A.T.M.';
            }
        },
        calendar : {
            sameDay : '[Hodiaŭ je] LT',
            nextDay : '[Morgaŭ je] LT',
            nextWeek : 'dddd [je] LT',
            lastDay : '[Hieraŭ je] LT',
            lastWeek : '[pasinta] dddd [je] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'post %s',
            past : 'antaŭ %s',
            s : 'sekundoj',
            ss : '%d sekundoj',
            m : 'minuto',
            mm : '%d minutoj',
            h : 'horo',
            hh : '%d horoj',
            d : 'tago',//ne 'diurno', ĉar estas uzita por proksimumo
            dd : '%d tagoj',
            M : 'monato',
            MM : '%d monatoj',
            y : 'jaro',
            yy : '%d jaroj'
        },
        dayOfMonthOrdinalParse: /\d{1,2}a/,
        ordinal : '%da',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortDot = 'ene._feb._mar._abr._may._jun._jul._ago._sep._oct._nov._dic.'.split('_'),
        monthsShort$1 = 'ene_feb_mar_abr_may_jun_jul_ago_sep_oct_nov_dic'.split('_');

    var monthsParse$1 = [/^ene/i, /^feb/i, /^mar/i, /^abr/i, /^may/i, /^jun/i, /^jul/i, /^ago/i, /^sep/i, /^oct/i, /^nov/i, /^dic/i];
    var monthsRegex$2 = /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i;

    hooks.defineLocale('es-do', {
        months : 'enero_febrero_marzo_abril_mayo_junio_julio_agosto_septiembre_octubre_noviembre_diciembre'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortDot;
            } else if (/-MMM-/.test(format)) {
                return monthsShort$1[m.month()];
            } else {
                return monthsShortDot[m.month()];
            }
        },
        monthsRegex: monthsRegex$2,
        monthsShortRegex: monthsRegex$2,
        monthsStrictRegex: /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)/i,
        monthsShortStrictRegex: /^(ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i,
        monthsParse: monthsParse$1,
        longMonthsParse: monthsParse$1,
        shortMonthsParse: monthsParse$1,
        weekdays : 'domingo_lunes_martes_miércoles_jueves_viernes_sábado'.split('_'),
        weekdaysShort : 'dom._lun._mar._mié._jue._vie._sáb.'.split('_'),
        weekdaysMin : 'do_lu_ma_mi_ju_vi_sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY h:mm A',
            LLLL : 'dddd, D [de] MMMM [de] YYYY h:mm A'
        },
        calendar : {
            sameDay : function () {
                return '[hoy a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextDay : function () {
                return '[mañana a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextWeek : function () {
                return 'dddd [a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastDay : function () {
                return '[ayer a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastWeek : function () {
                return '[el] dddd [pasado a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'en %s',
            past : 'hace %s',
            s : 'unos segundos',
            ss : '%d segundos',
            m : 'un minuto',
            mm : '%d minutos',
            h : 'una hora',
            hh : '%d horas',
            d : 'un día',
            dd : '%d días',
            M : 'un mes',
            MM : '%d meses',
            y : 'un año',
            yy : '%d años'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal : '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortDot$1 = 'ene._feb._mar._abr._may._jun._jul._ago._sep._oct._nov._dic.'.split('_'),
        monthsShort$2 = 'ene_feb_mar_abr_may_jun_jul_ago_sep_oct_nov_dic'.split('_');

    var monthsParse$2 = [/^ene/i, /^feb/i, /^mar/i, /^abr/i, /^may/i, /^jun/i, /^jul/i, /^ago/i, /^sep/i, /^oct/i, /^nov/i, /^dic/i];
    var monthsRegex$3 = /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i;

    hooks.defineLocale('es-us', {
        months : 'enero_febrero_marzo_abril_mayo_junio_julio_agosto_septiembre_octubre_noviembre_diciembre'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortDot$1;
            } else if (/-MMM-/.test(format)) {
                return monthsShort$2[m.month()];
            } else {
                return monthsShortDot$1[m.month()];
            }
        },
        monthsRegex: monthsRegex$3,
        monthsShortRegex: monthsRegex$3,
        monthsStrictRegex: /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)/i,
        monthsShortStrictRegex: /^(ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i,
        monthsParse: monthsParse$2,
        longMonthsParse: monthsParse$2,
        shortMonthsParse: monthsParse$2,
        weekdays : 'domingo_lunes_martes_miércoles_jueves_viernes_sábado'.split('_'),
        weekdaysShort : 'dom._lun._mar._mié._jue._vie._sáb.'.split('_'),
        weekdaysMin : 'do_lu_ma_mi_ju_vi_sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'MM/DD/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY h:mm A',
            LLLL : 'dddd, D [de] MMMM [de] YYYY h:mm A'
        },
        calendar : {
            sameDay : function () {
                return '[hoy a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextDay : function () {
                return '[mañana a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextWeek : function () {
                return 'dddd [a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastDay : function () {
                return '[ayer a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastWeek : function () {
                return '[el] dddd [pasado a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'en %s',
            past : 'hace %s',
            s : 'unos segundos',
            ss : '%d segundos',
            m : 'un minuto',
            mm : '%d minutos',
            h : 'una hora',
            hh : '%d horas',
            d : 'un día',
            dd : '%d días',
            M : 'un mes',
            MM : '%d meses',
            y : 'un año',
            yy : '%d años'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal : '%dº',
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortDot$2 = 'ene._feb._mar._abr._may._jun._jul._ago._sep._oct._nov._dic.'.split('_'),
        monthsShort$3 = 'ene_feb_mar_abr_may_jun_jul_ago_sep_oct_nov_dic'.split('_');

    var monthsParse$3 = [/^ene/i, /^feb/i, /^mar/i, /^abr/i, /^may/i, /^jun/i, /^jul/i, /^ago/i, /^sep/i, /^oct/i, /^nov/i, /^dic/i];
    var monthsRegex$4 = /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i;

    hooks.defineLocale('es', {
        months : 'enero_febrero_marzo_abril_mayo_junio_julio_agosto_septiembre_octubre_noviembre_diciembre'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortDot$2;
            } else if (/-MMM-/.test(format)) {
                return monthsShort$3[m.month()];
            } else {
                return monthsShortDot$2[m.month()];
            }
        },
        monthsRegex : monthsRegex$4,
        monthsShortRegex : monthsRegex$4,
        monthsStrictRegex : /^(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)/i,
        monthsShortStrictRegex : /^(ene\.?|feb\.?|mar\.?|abr\.?|may\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)/i,
        monthsParse : monthsParse$3,
        longMonthsParse : monthsParse$3,
        shortMonthsParse : monthsParse$3,
        weekdays : 'domingo_lunes_martes_miércoles_jueves_viernes_sábado'.split('_'),
        weekdaysShort : 'dom._lun._mar._mié._jue._vie._sáb.'.split('_'),
        weekdaysMin : 'do_lu_ma_mi_ju_vi_sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY H:mm',
            LLLL : 'dddd, D [de] MMMM [de] YYYY H:mm'
        },
        calendar : {
            sameDay : function () {
                return '[hoy a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextDay : function () {
                return '[mañana a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            nextWeek : function () {
                return 'dddd [a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastDay : function () {
                return '[ayer a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            lastWeek : function () {
                return '[el] dddd [pasado a la' + ((this.hours() !== 1) ? 's' : '') + '] LT';
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'en %s',
            past : 'hace %s',
            s : 'unos segundos',
            ss : '%d segundos',
            m : 'un minuto',
            mm : '%d minutos',
            h : 'una hora',
            hh : '%d horas',
            d : 'un día',
            dd : '%d días',
            M : 'un mes',
            MM : '%d meses',
            y : 'un año',
            yy : '%d años'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal : '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$3(number, withoutSuffix, key, isFuture) {
        var format = {
            's' : ['mõne sekundi', 'mõni sekund', 'paar sekundit'],
            'ss': [number + 'sekundi', number + 'sekundit'],
            'm' : ['ühe minuti', 'üks minut'],
            'mm': [number + ' minuti', number + ' minutit'],
            'h' : ['ühe tunni', 'tund aega', 'üks tund'],
            'hh': [number + ' tunni', number + ' tundi'],
            'd' : ['ühe päeva', 'üks päev'],
            'M' : ['kuu aja', 'kuu aega', 'üks kuu'],
            'MM': [number + ' kuu', number + ' kuud'],
            'y' : ['ühe aasta', 'aasta', 'üks aasta'],
            'yy': [number + ' aasta', number + ' aastat']
        };
        if (withoutSuffix) {
            return format[key][2] ? format[key][2] : format[key][1];
        }
        return isFuture ? format[key][0] : format[key][1];
    }

    hooks.defineLocale('et', {
        months        : 'jaanuar_veebruar_märts_aprill_mai_juuni_juuli_august_september_oktoober_november_detsember'.split('_'),
        monthsShort   : 'jaan_veebr_märts_apr_mai_juuni_juuli_aug_sept_okt_nov_dets'.split('_'),
        weekdays      : 'pühapäev_esmaspäev_teisipäev_kolmapäev_neljapäev_reede_laupäev'.split('_'),
        weekdaysShort : 'P_E_T_K_N_R_L'.split('_'),
        weekdaysMin   : 'P_E_T_K_N_R_L'.split('_'),
        longDateFormat : {
            LT   : 'H:mm',
            LTS : 'H:mm:ss',
            L    : 'DD.MM.YYYY',
            LL   : 'D. MMMM YYYY',
            LLL  : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd, D. MMMM YYYY H:mm'
        },
        calendar : {
            sameDay  : '[Täna,] LT',
            nextDay  : '[Homme,] LT',
            nextWeek : '[Järgmine] dddd LT',
            lastDay  : '[Eile,] LT',
            lastWeek : '[Eelmine] dddd LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s pärast',
            past   : '%s tagasi',
            s      : processRelativeTime$3,
            ss     : processRelativeTime$3,
            m      : processRelativeTime$3,
            mm     : processRelativeTime$3,
            h      : processRelativeTime$3,
            hh     : processRelativeTime$3,
            d      : processRelativeTime$3,
            dd     : '%d päeva',
            M      : processRelativeTime$3,
            MM     : processRelativeTime$3,
            y      : processRelativeTime$3,
            yy     : processRelativeTime$3
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('eu', {
        months : 'urtarrila_otsaila_martxoa_apirila_maiatza_ekaina_uztaila_abuztua_iraila_urria_azaroa_abendua'.split('_'),
        monthsShort : 'urt._ots._mar._api._mai._eka._uzt._abu._ira._urr._aza._abe.'.split('_'),
        monthsParseExact : true,
        weekdays : 'igandea_astelehena_asteartea_asteazkena_osteguna_ostirala_larunbata'.split('_'),
        weekdaysShort : 'ig._al._ar._az._og._ol._lr.'.split('_'),
        weekdaysMin : 'ig_al_ar_az_og_ol_lr'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'YYYY[ko] MMMM[ren] D[a]',
            LLL : 'YYYY[ko] MMMM[ren] D[a] HH:mm',
            LLLL : 'dddd, YYYY[ko] MMMM[ren] D[a] HH:mm',
            l : 'YYYY-M-D',
            ll : 'YYYY[ko] MMM D[a]',
            lll : 'YYYY[ko] MMM D[a] HH:mm',
            llll : 'ddd, YYYY[ko] MMM D[a] HH:mm'
        },
        calendar : {
            sameDay : '[gaur] LT[etan]',
            nextDay : '[bihar] LT[etan]',
            nextWeek : 'dddd LT[etan]',
            lastDay : '[atzo] LT[etan]',
            lastWeek : '[aurreko] dddd LT[etan]',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s barru',
            past : 'duela %s',
            s : 'segundo batzuk',
            ss : '%d segundo',
            m : 'minutu bat',
            mm : '%d minutu',
            h : 'ordu bat',
            hh : '%d ordu',
            d : 'egun bat',
            dd : '%d egun',
            M : 'hilabete bat',
            MM : '%d hilabete',
            y : 'urte bat',
            yy : '%d urte'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$5 = {
        '1': '۱',
        '2': '۲',
        '3': '۳',
        '4': '۴',
        '5': '۵',
        '6': '۶',
        '7': '۷',
        '8': '۸',
        '9': '۹',
        '0': '۰'
    }, numberMap$4 = {
        '۱': '1',
        '۲': '2',
        '۳': '3',
        '۴': '4',
        '۵': '5',
        '۶': '6',
        '۷': '7',
        '۸': '8',
        '۹': '9',
        '۰': '0'
    };

    hooks.defineLocale('fa', {
        months : 'ژانویه_فوریه_مارس_آوریل_مه_ژوئن_ژوئیه_اوت_سپتامبر_اکتبر_نوامبر_دسامبر'.split('_'),
        monthsShort : 'ژانویه_فوریه_مارس_آوریل_مه_ژوئن_ژوئیه_اوت_سپتامبر_اکتبر_نوامبر_دسامبر'.split('_'),
        weekdays : 'یک\u200cشنبه_دوشنبه_سه\u200cشنبه_چهارشنبه_پنج\u200cشنبه_جمعه_شنبه'.split('_'),
        weekdaysShort : 'یک\u200cشنبه_دوشنبه_سه\u200cشنبه_چهارشنبه_پنج\u200cشنبه_جمعه_شنبه'.split('_'),
        weekdaysMin : 'ی_د_س_چ_پ_ج_ش'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        meridiemParse: /قبل از ظهر|بعد از ظهر/,
        isPM: function (input) {
            return /بعد از ظهر/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'قبل از ظهر';
            } else {
                return 'بعد از ظهر';
            }
        },
        calendar : {
            sameDay : '[امروز ساعت] LT',
            nextDay : '[فردا ساعت] LT',
            nextWeek : 'dddd [ساعت] LT',
            lastDay : '[دیروز ساعت] LT',
            lastWeek : 'dddd [پیش] [ساعت] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'در %s',
            past : '%s پیش',
            s : 'چند ثانیه',
            ss : 'ثانیه d%',
            m : 'یک دقیقه',
            mm : '%d دقیقه',
            h : 'یک ساعت',
            hh : '%d ساعت',
            d : 'یک روز',
            dd : '%d روز',
            M : 'یک ماه',
            MM : '%d ماه',
            y : 'یک سال',
            yy : '%d سال'
        },
        preparse: function (string) {
            return string.replace(/[۰-۹]/g, function (match) {
                return numberMap$4[match];
            }).replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$5[match];
            }).replace(/,/g, '،');
        },
        dayOfMonthOrdinalParse: /\d{1,2}م/,
        ordinal : '%dم',
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12 // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var numbersPast = 'nolla yksi kaksi kolme neljä viisi kuusi seitsemän kahdeksan yhdeksän'.split(' '),
        numbersFuture = [
            'nolla', 'yhden', 'kahden', 'kolmen', 'neljän', 'viiden', 'kuuden',
            numbersPast[7], numbersPast[8], numbersPast[9]
        ];
    function translate$2(number, withoutSuffix, key, isFuture) {
        var result = '';
        switch (key) {
            case 's':
                return isFuture ? 'muutaman sekunnin' : 'muutama sekunti';
            case 'ss':
                return isFuture ? 'sekunnin' : 'sekuntia';
            case 'm':
                return isFuture ? 'minuutin' : 'minuutti';
            case 'mm':
                result = isFuture ? 'minuutin' : 'minuuttia';
                break;
            case 'h':
                return isFuture ? 'tunnin' : 'tunti';
            case 'hh':
                result = isFuture ? 'tunnin' : 'tuntia';
                break;
            case 'd':
                return isFuture ? 'päivän' : 'päivä';
            case 'dd':
                result = isFuture ? 'päivän' : 'päivää';
                break;
            case 'M':
                return isFuture ? 'kuukauden' : 'kuukausi';
            case 'MM':
                result = isFuture ? 'kuukauden' : 'kuukautta';
                break;
            case 'y':
                return isFuture ? 'vuoden' : 'vuosi';
            case 'yy':
                result = isFuture ? 'vuoden' : 'vuotta';
                break;
        }
        result = verbalNumber(number, isFuture) + ' ' + result;
        return result;
    }
    function verbalNumber(number, isFuture) {
        return number < 10 ? (isFuture ? numbersFuture[number] : numbersPast[number]) : number;
    }

    hooks.defineLocale('fi', {
        months : 'tammikuu_helmikuu_maaliskuu_huhtikuu_toukokuu_kesäkuu_heinäkuu_elokuu_syyskuu_lokakuu_marraskuu_joulukuu'.split('_'),
        monthsShort : 'tammi_helmi_maalis_huhti_touko_kesä_heinä_elo_syys_loka_marras_joulu'.split('_'),
        weekdays : 'sunnuntai_maanantai_tiistai_keskiviikko_torstai_perjantai_lauantai'.split('_'),
        weekdaysShort : 'su_ma_ti_ke_to_pe_la'.split('_'),
        weekdaysMin : 'su_ma_ti_ke_to_pe_la'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD.MM.YYYY',
            LL : 'Do MMMM[ta] YYYY',
            LLL : 'Do MMMM[ta] YYYY, [klo] HH.mm',
            LLLL : 'dddd, Do MMMM[ta] YYYY, [klo] HH.mm',
            l : 'D.M.YYYY',
            ll : 'Do MMM YYYY',
            lll : 'Do MMM YYYY, [klo] HH.mm',
            llll : 'ddd, Do MMM YYYY, [klo] HH.mm'
        },
        calendar : {
            sameDay : '[tänään] [klo] LT',
            nextDay : '[huomenna] [klo] LT',
            nextWeek : 'dddd [klo] LT',
            lastDay : '[eilen] [klo] LT',
            lastWeek : '[viime] dddd[na] [klo] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s päästä',
            past : '%s sitten',
            s : translate$2,
            ss : translate$2,
            m : translate$2,
            mm : translate$2,
            h : translate$2,
            hh : translate$2,
            d : translate$2,
            dd : translate$2,
            M : translate$2,
            MM : translate$2,
            y : translate$2,
            yy : translate$2
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('fo', {
        months : 'januar_februar_mars_apríl_mai_juni_juli_august_september_oktober_november_desember'.split('_'),
        monthsShort : 'jan_feb_mar_apr_mai_jun_jul_aug_sep_okt_nov_des'.split('_'),
        weekdays : 'sunnudagur_mánadagur_týsdagur_mikudagur_hósdagur_fríggjadagur_leygardagur'.split('_'),
        weekdaysShort : 'sun_mán_týs_mik_hós_frí_ley'.split('_'),
        weekdaysMin : 'su_má_tý_mi_hó_fr_le'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D. MMMM, YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Í dag kl.] LT',
            nextDay : '[Í morgin kl.] LT',
            nextWeek : 'dddd [kl.] LT',
            lastDay : '[Í gjár kl.] LT',
            lastWeek : '[síðstu] dddd [kl] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'um %s',
            past : '%s síðani',
            s : 'fá sekund',
            ss : '%d sekundir',
            m : 'ein minuttur',
            mm : '%d minuttir',
            h : 'ein tími',
            hh : '%d tímar',
            d : 'ein dagur',
            dd : '%d dagar',
            M : 'ein mánaður',
            MM : '%d mánaðir',
            y : 'eitt ár',
            yy : '%d ár'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('fr-ca', {
        months : 'janvier_février_mars_avril_mai_juin_juillet_août_septembre_octobre_novembre_décembre'.split('_'),
        monthsShort : 'janv._févr._mars_avr._mai_juin_juil._août_sept._oct._nov._déc.'.split('_'),
        monthsParseExact : true,
        weekdays : 'dimanche_lundi_mardi_mercredi_jeudi_vendredi_samedi'.split('_'),
        weekdaysShort : 'dim._lun._mar._mer._jeu._ven._sam.'.split('_'),
        weekdaysMin : 'di_lu_ma_me_je_ve_sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Aujourd’hui à] LT',
            nextDay : '[Demain à] LT',
            nextWeek : 'dddd [à] LT',
            lastDay : '[Hier à] LT',
            lastWeek : 'dddd [dernier à] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dans %s',
            past : 'il y a %s',
            s : 'quelques secondes',
            ss : '%d secondes',
            m : 'une minute',
            mm : '%d minutes',
            h : 'une heure',
            hh : '%d heures',
            d : 'un jour',
            dd : '%d jours',
            M : 'un mois',
            MM : '%d mois',
            y : 'un an',
            yy : '%d ans'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(er|e)/,
        ordinal : function (number, period) {
            switch (period) {
                // Words with masculine grammatical gender: mois, trimestre, jour
                default:
                case 'M':
                case 'Q':
                case 'D':
                case 'DDD':
                case 'd':
                    return number + (number === 1 ? 'er' : 'e');

                // Words with feminine grammatical gender: semaine
                case 'w':
                case 'W':
                    return number + (number === 1 ? 're' : 'e');
            }
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('fr-ch', {
        months : 'janvier_février_mars_avril_mai_juin_juillet_août_septembre_octobre_novembre_décembre'.split('_'),
        monthsShort : 'janv._févr._mars_avr._mai_juin_juil._août_sept._oct._nov._déc.'.split('_'),
        monthsParseExact : true,
        weekdays : 'dimanche_lundi_mardi_mercredi_jeudi_vendredi_samedi'.split('_'),
        weekdaysShort : 'dim._lun._mar._mer._jeu._ven._sam.'.split('_'),
        weekdaysMin : 'di_lu_ma_me_je_ve_sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Aujourd’hui à] LT',
            nextDay : '[Demain à] LT',
            nextWeek : 'dddd [à] LT',
            lastDay : '[Hier à] LT',
            lastWeek : 'dddd [dernier à] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dans %s',
            past : 'il y a %s',
            s : 'quelques secondes',
            ss : '%d secondes',
            m : 'une minute',
            mm : '%d minutes',
            h : 'une heure',
            hh : '%d heures',
            d : 'un jour',
            dd : '%d jours',
            M : 'un mois',
            MM : '%d mois',
            y : 'un an',
            yy : '%d ans'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(er|e)/,
        ordinal : function (number, period) {
            switch (period) {
                // Words with masculine grammatical gender: mois, trimestre, jour
                default:
                case 'M':
                case 'Q':
                case 'D':
                case 'DDD':
                case 'd':
                    return number + (number === 1 ? 'er' : 'e');

                // Words with feminine grammatical gender: semaine
                case 'w':
                case 'W':
                    return number + (number === 1 ? 're' : 'e');
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('fr', {
        months : 'janvier_février_mars_avril_mai_juin_juillet_août_septembre_octobre_novembre_décembre'.split('_'),
        monthsShort : 'janv._févr._mars_avr._mai_juin_juil._août_sept._oct._nov._déc.'.split('_'),
        monthsParseExact : true,
        weekdays : 'dimanche_lundi_mardi_mercredi_jeudi_vendredi_samedi'.split('_'),
        weekdaysShort : 'dim._lun._mar._mer._jeu._ven._sam.'.split('_'),
        weekdaysMin : 'di_lu_ma_me_je_ve_sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Aujourd’hui à] LT',
            nextDay : '[Demain à] LT',
            nextWeek : 'dddd [à] LT',
            lastDay : '[Hier à] LT',
            lastWeek : 'dddd [dernier à] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dans %s',
            past : 'il y a %s',
            s : 'quelques secondes',
            ss : '%d secondes',
            m : 'une minute',
            mm : '%d minutes',
            h : 'une heure',
            hh : '%d heures',
            d : 'un jour',
            dd : '%d jours',
            M : 'un mois',
            MM : '%d mois',
            y : 'un an',
            yy : '%d ans'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(er|)/,
        ordinal : function (number, period) {
            switch (period) {
                // TODO: Return 'e' when day of month > 1. Move this case inside
                // block for masculine words below.
                // See https://github.com/moment/moment/issues/3375
                case 'D':
                    return number + (number === 1 ? 'er' : '');

                // Words with masculine grammatical gender: mois, trimestre, jour
                default:
                case 'M':
                case 'Q':
                case 'DDD':
                case 'd':
                    return number + (number === 1 ? 'er' : 'e');

                // Words with feminine grammatical gender: semaine
                case 'w':
                case 'W':
                    return number + (number === 1 ? 're' : 'e');
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortWithDots = 'jan._feb._mrt._apr._mai_jun._jul._aug._sep._okt._nov._des.'.split('_'),
        monthsShortWithoutDots = 'jan_feb_mrt_apr_mai_jun_jul_aug_sep_okt_nov_des'.split('_');

    hooks.defineLocale('fy', {
        months : 'jannewaris_febrewaris_maart_april_maaie_juny_july_augustus_septimber_oktober_novimber_desimber'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortWithDots;
            } else if (/-MMM-/.test(format)) {
                return monthsShortWithoutDots[m.month()];
            } else {
                return monthsShortWithDots[m.month()];
            }
        },
        monthsParseExact : true,
        weekdays : 'snein_moandei_tiisdei_woansdei_tongersdei_freed_sneon'.split('_'),
        weekdaysShort : 'si._mo._ti._wo._to._fr._so.'.split('_'),
        weekdaysMin : 'Si_Mo_Ti_Wo_To_Fr_So'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD-MM-YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[hjoed om] LT',
            nextDay: '[moarn om] LT',
            nextWeek: 'dddd [om] LT',
            lastDay: '[juster om] LT',
            lastWeek: '[ôfrûne] dddd [om] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'oer %s',
            past : '%s lyn',
            s : 'in pear sekonden',
            ss : '%d sekonden',
            m : 'ien minút',
            mm : '%d minuten',
            h : 'ien oere',
            hh : '%d oeren',
            d : 'ien dei',
            dd : '%d dagen',
            M : 'ien moanne',
            MM : '%d moannen',
            y : 'ien jier',
            yy : '%d jierren'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(ste|de)/,
        ordinal : function (number) {
            return number + ((number === 1 || number === 8 || number >= 20) ? 'ste' : 'de');
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration


    var months$5 = [
        'Eanáir', 'Feabhra', 'Márta', 'Aibreán', 'Bealtaine', 'Méitheamh', 'Iúil', 'Lúnasa', 'Meán Fómhair', 'Deaireadh Fómhair', 'Samhain', 'Nollaig'
    ];

    var monthsShort$4 = ['Eaná', 'Feab', 'Márt', 'Aibr', 'Beal', 'Méit', 'Iúil', 'Lúna', 'Meán', 'Deai', 'Samh', 'Noll'];

    var weekdays$1 = ['Dé Domhnaigh', 'Dé Luain', 'Dé Máirt', 'Dé Céadaoin', 'Déardaoin', 'Dé hAoine', 'Dé Satharn'];

    var weekdaysShort = ['Dom', 'Lua', 'Mái', 'Céa', 'Déa', 'hAo', 'Sat'];

    var weekdaysMin = ['Do', 'Lu', 'Má', 'Ce', 'Dé', 'hA', 'Sa'];

    hooks.defineLocale('ga', {
        months: months$5,
        monthsShort: monthsShort$4,
        monthsParseExact: true,
        weekdays: weekdays$1,
        weekdaysShort: weekdaysShort,
        weekdaysMin: weekdaysMin,
        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY HH:mm',
            LLLL: 'dddd, D MMMM YYYY HH:mm'
        },
        calendar: {
            sameDay: '[Inniu ag] LT',
            nextDay: '[Amárach ag] LT',
            nextWeek: 'dddd [ag] LT',
            lastDay: '[Inné aig] LT',
            lastWeek: 'dddd [seo caite] [ag] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: 'i %s',
            past: '%s ó shin',
            s: 'cúpla soicind',
            ss: '%d soicind',
            m: 'nóiméad',
            mm: '%d nóiméad',
            h: 'uair an chloig',
            hh: '%d uair an chloig',
            d: 'lá',
            dd: '%d lá',
            M: 'mí',
            MM: '%d mí',
            y: 'bliain',
            yy: '%d bliain'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(d|na|mh)/,
        ordinal: function (number) {
            var output = number === 1 ? 'd' : number % 10 === 2 ? 'na' : 'mh';
            return number + output;
        },
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var months$6 = [
        'Am Faoilleach', 'An Gearran', 'Am Màrt', 'An Giblean', 'An Cèitean', 'An t-Ògmhios', 'An t-Iuchar', 'An Lùnastal', 'An t-Sultain', 'An Dàmhair', 'An t-Samhain', 'An Dùbhlachd'
    ];

    var monthsShort$5 = ['Faoi', 'Gear', 'Màrt', 'Gibl', 'Cèit', 'Ògmh', 'Iuch', 'Lùn', 'Sult', 'Dàmh', 'Samh', 'Dùbh'];

    var weekdays$2 = ['Didòmhnaich', 'Diluain', 'Dimàirt', 'Diciadain', 'Diardaoin', 'Dihaoine', 'Disathairne'];

    var weekdaysShort$1 = ['Did', 'Dil', 'Dim', 'Dic', 'Dia', 'Dih', 'Dis'];

    var weekdaysMin$1 = ['Dò', 'Lu', 'Mà', 'Ci', 'Ar', 'Ha', 'Sa'];

    hooks.defineLocale('gd', {
        months : months$6,
        monthsShort : monthsShort$5,
        monthsParseExact : true,
        weekdays : weekdays$2,
        weekdaysShort : weekdaysShort$1,
        weekdaysMin : weekdaysMin$1,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[An-diugh aig] LT',
            nextDay : '[A-màireach aig] LT',
            nextWeek : 'dddd [aig] LT',
            lastDay : '[An-dè aig] LT',
            lastWeek : 'dddd [seo chaidh] [aig] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'ann an %s',
            past : 'bho chionn %s',
            s : 'beagan diogan',
            ss : '%d diogan',
            m : 'mionaid',
            mm : '%d mionaidean',
            h : 'uair',
            hh : '%d uairean',
            d : 'latha',
            dd : '%d latha',
            M : 'mìos',
            MM : '%d mìosan',
            y : 'bliadhna',
            yy : '%d bliadhna'
        },
        dayOfMonthOrdinalParse : /\d{1,2}(d|na|mh)/,
        ordinal : function (number) {
            var output = number === 1 ? 'd' : number % 10 === 2 ? 'na' : 'mh';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('gl', {
        months : 'xaneiro_febreiro_marzo_abril_maio_xuño_xullo_agosto_setembro_outubro_novembro_decembro'.split('_'),
        monthsShort : 'xan._feb._mar._abr._mai._xuñ._xul._ago._set._out._nov._dec.'.split('_'),
        monthsParseExact: true,
        weekdays : 'domingo_luns_martes_mércores_xoves_venres_sábado'.split('_'),
        weekdaysShort : 'dom._lun._mar._mér._xov._ven._sáb.'.split('_'),
        weekdaysMin : 'do_lu_ma_mé_xo_ve_sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY H:mm',
            LLLL : 'dddd, D [de] MMMM [de] YYYY H:mm'
        },
        calendar : {
            sameDay : function () {
                return '[hoxe ' + ((this.hours() !== 1) ? 'ás' : 'á') + '] LT';
            },
            nextDay : function () {
                return '[mañá ' + ((this.hours() !== 1) ? 'ás' : 'á') + '] LT';
            },
            nextWeek : function () {
                return 'dddd [' + ((this.hours() !== 1) ? 'ás' : 'a') + '] LT';
            },
            lastDay : function () {
                return '[onte ' + ((this.hours() !== 1) ? 'á' : 'a') + '] LT';
            },
            lastWeek : function () {
                return '[o] dddd [pasado ' + ((this.hours() !== 1) ? 'ás' : 'a') + '] LT';
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : function (str) {
                if (str.indexOf('un') === 0) {
                    return 'n' + str;
                }
                return 'en ' + str;
            },
            past : 'hai %s',
            s : 'uns segundos',
            ss : '%d segundos',
            m : 'un minuto',
            mm : '%d minutos',
            h : 'unha hora',
            hh : '%d horas',
            d : 'un día',
            dd : '%d días',
            M : 'un mes',
            MM : '%d meses',
            y : 'un ano',
            yy : '%d anos'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal : '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$4(number, withoutSuffix, key, isFuture) {
        var format = {
            's': ['thodde secondanim', 'thodde second'],
            'ss': [number + ' secondanim', number + ' second'],
            'm': ['eka mintan', 'ek minute'],
            'mm': [number + ' mintanim', number + ' mintam'],
            'h': ['eka voran', 'ek vor'],
            'hh': [number + ' voranim', number + ' voram'],
            'd': ['eka disan', 'ek dis'],
            'dd': [number + ' disanim', number + ' dis'],
            'M': ['eka mhoinean', 'ek mhoino'],
            'MM': [number + ' mhoineanim', number + ' mhoine'],
            'y': ['eka vorsan', 'ek voros'],
            'yy': [number + ' vorsanim', number + ' vorsam']
        };
        return withoutSuffix ? format[key][0] : format[key][1];
    }

    hooks.defineLocale('gom-latn', {
        months : 'Janer_Febrer_Mars_Abril_Mai_Jun_Julai_Agost_Setembr_Otubr_Novembr_Dezembr'.split('_'),
        monthsShort : 'Jan._Feb._Mars_Abr._Mai_Jun_Jul._Ago._Set._Otu._Nov._Dez.'.split('_'),
        monthsParseExact : true,
        weekdays : 'Aitar_Somar_Mongllar_Budvar_Brestar_Sukrar_Son\'var'.split('_'),
        weekdaysShort : 'Ait._Som._Mon._Bud._Bre._Suk._Son.'.split('_'),
        weekdaysMin : 'Ai_Sm_Mo_Bu_Br_Su_Sn'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'A h:mm [vazta]',
            LTS : 'A h:mm:ss [vazta]',
            L : 'DD-MM-YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY A h:mm [vazta]',
            LLLL : 'dddd, MMMM[achea] Do, YYYY, A h:mm [vazta]',
            llll: 'ddd, D MMM YYYY, A h:mm [vazta]'
        },
        calendar : {
            sameDay: '[Aiz] LT',
            nextDay: '[Faleam] LT',
            nextWeek: '[Ieta to] dddd[,] LT',
            lastDay: '[Kal] LT',
            lastWeek: '[Fatlo] dddd[,] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : '%s',
            past : '%s adim',
            s : processRelativeTime$4,
            ss : processRelativeTime$4,
            m : processRelativeTime$4,
            mm : processRelativeTime$4,
            h : processRelativeTime$4,
            hh : processRelativeTime$4,
            d : processRelativeTime$4,
            dd : processRelativeTime$4,
            M : processRelativeTime$4,
            MM : processRelativeTime$4,
            y : processRelativeTime$4,
            yy : processRelativeTime$4
        },
        dayOfMonthOrdinalParse : /\d{1,2}(er)/,
        ordinal : function (number, period) {
            switch (period) {
                // the ordinal 'er' only applies to day of the month
                case 'D':
                    return number + 'er';
                default:
                case 'M':
                case 'Q':
                case 'DDD':
                case 'd':
                case 'w':
                case 'W':
                    return number;
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        },
        meridiemParse: /rati|sokalli|donparam|sanje/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'rati') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'sokalli') {
                return hour;
            } else if (meridiem === 'donparam') {
                return hour > 12 ? hour : hour + 12;
            } else if (meridiem === 'sanje') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'rati';
            } else if (hour < 12) {
                return 'sokalli';
            } else if (hour < 16) {
                return 'donparam';
            } else if (hour < 20) {
                return 'sanje';
            } else {
                return 'rati';
            }
        }
    });

    //! moment.js locale configuration

    var symbolMap$6 = {
            '1': '૧',
            '2': '૨',
            '3': '૩',
            '4': '૪',
            '5': '૫',
            '6': '૬',
            '7': '૭',
            '8': '૮',
            '9': '૯',
            '0': '૦'
        },
        numberMap$5 = {
            '૧': '1',
            '૨': '2',
            '૩': '3',
            '૪': '4',
            '૫': '5',
            '૬': '6',
            '૭': '7',
            '૮': '8',
            '૯': '9',
            '૦': '0'
        };

    hooks.defineLocale('gu', {
        months: 'જાન્યુઆરી_ફેબ્રુઆરી_માર્ચ_એપ્રિલ_મે_જૂન_જુલાઈ_ઑગસ્ટ_સપ્ટેમ્બર_ઑક્ટ્બર_નવેમ્બર_ડિસેમ્બર'.split('_'),
        monthsShort: 'જાન્યુ._ફેબ્રુ._માર્ચ_એપ્રિ._મે_જૂન_જુલા._ઑગ._સપ્ટે._ઑક્ટ્._નવે._ડિસે.'.split('_'),
        monthsParseExact: true,
        weekdays: 'રવિવાર_સોમવાર_મંગળવાર_બુધ્વાર_ગુરુવાર_શુક્રવાર_શનિવાર'.split('_'),
        weekdaysShort: 'રવિ_સોમ_મંગળ_બુધ્_ગુરુ_શુક્ર_શનિ'.split('_'),
        weekdaysMin: 'ર_સો_મં_બુ_ગુ_શુ_શ'.split('_'),
        longDateFormat: {
            LT: 'A h:mm વાગ્યે',
            LTS: 'A h:mm:ss વાગ્યે',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY, A h:mm વાગ્યે',
            LLLL: 'dddd, D MMMM YYYY, A h:mm વાગ્યે'
        },
        calendar: {
            sameDay: '[આજ] LT',
            nextDay: '[કાલે] LT',
            nextWeek: 'dddd, LT',
            lastDay: '[ગઇકાલે] LT',
            lastWeek: '[પાછલા] dddd, LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: '%s મા',
            past: '%s પેહલા',
            s: 'અમુક પળો',
            ss: '%d સેકંડ',
            m: 'એક મિનિટ',
            mm: '%d મિનિટ',
            h: 'એક કલાક',
            hh: '%d કલાક',
            d: 'એક દિવસ',
            dd: '%d દિવસ',
            M: 'એક મહિનો',
            MM: '%d મહિનો',
            y: 'એક વર્ષ',
            yy: '%d વર્ષ'
        },
        preparse: function (string) {
            return string.replace(/[૧૨૩૪૫૬૭૮૯૦]/g, function (match) {
                return numberMap$5[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$6[match];
            });
        },
        // Gujarati notation for meridiems are quite fuzzy in practice. While there exists
        // a rigid notion of a 'Pahar' it is not used as rigidly in modern Gujarati.
        meridiemParse: /રાત|બપોર|સવાર|સાંજ/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'રાત') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'સવાર') {
                return hour;
            } else if (meridiem === 'બપોર') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'સાંજ') {
                return hour + 12;
            }
        },
        meridiem: function (hour, minute, isLower) {
            if (hour < 4) {
                return 'રાત';
            } else if (hour < 10) {
                return 'સવાર';
            } else if (hour < 17) {
                return 'બપોર';
            } else if (hour < 20) {
                return 'સાંજ';
            } else {
                return 'રાત';
            }
        },
        week: {
            dow: 0, // Sunday is the first day of the week.
            doy: 6 // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('he', {
        months : 'ינואר_פברואר_מרץ_אפריל_מאי_יוני_יולי_אוגוסט_ספטמבר_אוקטובר_נובמבר_דצמבר'.split('_'),
        monthsShort : 'ינו׳_פבר׳_מרץ_אפר׳_מאי_יוני_יולי_אוג׳_ספט׳_אוק׳_נוב׳_דצמ׳'.split('_'),
        weekdays : 'ראשון_שני_שלישי_רביעי_חמישי_שישי_שבת'.split('_'),
        weekdaysShort : 'א׳_ב׳_ג׳_ד׳_ה׳_ו׳_ש׳'.split('_'),
        weekdaysMin : 'א_ב_ג_ד_ה_ו_ש'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D [ב]MMMM YYYY',
            LLL : 'D [ב]MMMM YYYY HH:mm',
            LLLL : 'dddd, D [ב]MMMM YYYY HH:mm',
            l : 'D/M/YYYY',
            ll : 'D MMM YYYY',
            lll : 'D MMM YYYY HH:mm',
            llll : 'ddd, D MMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[היום ב־]LT',
            nextDay : '[מחר ב־]LT',
            nextWeek : 'dddd [בשעה] LT',
            lastDay : '[אתמול ב־]LT',
            lastWeek : '[ביום] dddd [האחרון בשעה] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'בעוד %s',
            past : 'לפני %s',
            s : 'מספר שניות',
            ss : '%d שניות',
            m : 'דקה',
            mm : '%d דקות',
            h : 'שעה',
            hh : function (number) {
                if (number === 2) {
                    return 'שעתיים';
                }
                return number + ' שעות';
            },
            d : 'יום',
            dd : function (number) {
                if (number === 2) {
                    return 'יומיים';
                }
                return number + ' ימים';
            },
            M : 'חודש',
            MM : function (number) {
                if (number === 2) {
                    return 'חודשיים';
                }
                return number + ' חודשים';
            },
            y : 'שנה',
            yy : function (number) {
                if (number === 2) {
                    return 'שנתיים';
                } else if (number % 10 === 0 && number !== 10) {
                    return number + ' שנה';
                }
                return number + ' שנים';
            }
        },
        meridiemParse: /אחה"צ|לפנה"צ|אחרי הצהריים|לפני הצהריים|לפנות בוקר|בבוקר|בערב/i,
        isPM : function (input) {
            return /^(אחה"צ|אחרי הצהריים|בערב)$/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 5) {
                return 'לפנות בוקר';
            } else if (hour < 10) {
                return 'בבוקר';
            } else if (hour < 12) {
                return isLower ? 'לפנה"צ' : 'לפני הצהריים';
            } else if (hour < 18) {
                return isLower ? 'אחה"צ' : 'אחרי הצהריים';
            } else {
                return 'בערב';
            }
        }
    });

    //! moment.js locale configuration

    var symbolMap$7 = {
        '1': '१',
        '2': '२',
        '3': '३',
        '4': '४',
        '5': '५',
        '6': '६',
        '7': '७',
        '8': '८',
        '9': '९',
        '0': '०'
    },
    numberMap$6 = {
        '१': '1',
        '२': '2',
        '३': '3',
        '४': '4',
        '५': '5',
        '६': '6',
        '७': '7',
        '८': '8',
        '९': '9',
        '०': '0'
    };

    hooks.defineLocale('hi', {
        months : 'जनवरी_फ़रवरी_मार्च_अप्रैल_मई_जून_जुलाई_अगस्त_सितम्बर_अक्टूबर_नवम्बर_दिसम्बर'.split('_'),
        monthsShort : 'जन._फ़र._मार्च_अप्रै._मई_जून_जुल._अग._सित._अक्टू._नव._दिस.'.split('_'),
        monthsParseExact: true,
        weekdays : 'रविवार_सोमवार_मंगलवार_बुधवार_गुरूवार_शुक्रवार_शनिवार'.split('_'),
        weekdaysShort : 'रवि_सोम_मंगल_बुध_गुरू_शुक्र_शनि'.split('_'),
        weekdaysMin : 'र_सो_मं_बु_गु_शु_श'.split('_'),
        longDateFormat : {
            LT : 'A h:mm बजे',
            LTS : 'A h:mm:ss बजे',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm बजे',
            LLLL : 'dddd, D MMMM YYYY, A h:mm बजे'
        },
        calendar : {
            sameDay : '[आज] LT',
            nextDay : '[कल] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[कल] LT',
            lastWeek : '[पिछले] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s में',
            past : '%s पहले',
            s : 'कुछ ही क्षण',
            ss : '%d सेकंड',
            m : 'एक मिनट',
            mm : '%d मिनट',
            h : 'एक घंटा',
            hh : '%d घंटे',
            d : 'एक दिन',
            dd : '%d दिन',
            M : 'एक महीने',
            MM : '%d महीने',
            y : 'एक वर्ष',
            yy : '%d वर्ष'
        },
        preparse: function (string) {
            return string.replace(/[१२३४५६७८९०]/g, function (match) {
                return numberMap$6[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$7[match];
            });
        },
        // Hindi notation for meridiems are quite fuzzy in practice. While there exists
        // a rigid notion of a 'Pahar' it is not used as rigidly in modern Hindi.
        meridiemParse: /रात|सुबह|दोपहर|शाम/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'रात') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'सुबह') {
                return hour;
            } else if (meridiem === 'दोपहर') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'शाम') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'रात';
            } else if (hour < 10) {
                return 'सुबह';
            } else if (hour < 17) {
                return 'दोपहर';
            } else if (hour < 20) {
                return 'शाम';
            } else {
                return 'रात';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function translate$3(number, withoutSuffix, key) {
        var result = number + ' ';
        switch (key) {
            case 'ss':
                if (number === 1) {
                    result += 'sekunda';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'sekunde';
                } else {
                    result += 'sekundi';
                }
                return result;
            case 'm':
                return withoutSuffix ? 'jedna minuta' : 'jedne minute';
            case 'mm':
                if (number === 1) {
                    result += 'minuta';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'minute';
                } else {
                    result += 'minuta';
                }
                return result;
            case 'h':
                return withoutSuffix ? 'jedan sat' : 'jednog sata';
            case 'hh':
                if (number === 1) {
                    result += 'sat';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'sata';
                } else {
                    result += 'sati';
                }
                return result;
            case 'dd':
                if (number === 1) {
                    result += 'dan';
                } else {
                    result += 'dana';
                }
                return result;
            case 'MM':
                if (number === 1) {
                    result += 'mjesec';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'mjeseca';
                } else {
                    result += 'mjeseci';
                }
                return result;
            case 'yy':
                if (number === 1) {
                    result += 'godina';
                } else if (number === 2 || number === 3 || number === 4) {
                    result += 'godine';
                } else {
                    result += 'godina';
                }
                return result;
        }
    }

    hooks.defineLocale('hr', {
        months : {
            format: 'siječnja_veljače_ožujka_travnja_svibnja_lipnja_srpnja_kolovoza_rujna_listopada_studenoga_prosinca'.split('_'),
            standalone: 'siječanj_veljača_ožujak_travanj_svibanj_lipanj_srpanj_kolovoz_rujan_listopad_studeni_prosinac'.split('_')
        },
        monthsShort : 'sij._velj._ožu._tra._svi._lip._srp._kol._ruj._lis._stu._pro.'.split('_'),
        monthsParseExact: true,
        weekdays : 'nedjelja_ponedjeljak_utorak_srijeda_četvrtak_petak_subota'.split('_'),
        weekdaysShort : 'ned._pon._uto._sri._čet._pet._sub.'.split('_'),
        weekdaysMin : 'ne_po_ut_sr_če_pe_su'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd, D. MMMM YYYY H:mm'
        },
        calendar : {
            sameDay  : '[danas u] LT',
            nextDay  : '[sutra u] LT',
            nextWeek : function () {
                switch (this.day()) {
                    case 0:
                        return '[u] [nedjelju] [u] LT';
                    case 3:
                        return '[u] [srijedu] [u] LT';
                    case 6:
                        return '[u] [subotu] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[u] dddd [u] LT';
                }
            },
            lastDay  : '[jučer u] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                        return '[prošlu] dddd [u] LT';
                    case 6:
                        return '[prošle] [subote] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[prošli] dddd [u] LT';
                }
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'za %s',
            past   : 'prije %s',
            s      : 'par sekundi',
            ss     : translate$3,
            m      : translate$3,
            mm     : translate$3,
            h      : translate$3,
            hh     : translate$3,
            d      : 'dan',
            dd     : translate$3,
            M      : 'mjesec',
            MM     : translate$3,
            y      : 'godinu',
            yy     : translate$3
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var weekEndings = 'vasárnap hétfőn kedden szerdán csütörtökön pénteken szombaton'.split(' ');
    function translate$4(number, withoutSuffix, key, isFuture) {
        var num = number;
        switch (key) {
            case 's':
                return (isFuture || withoutSuffix) ? 'néhány másodperc' : 'néhány másodperce';
            case 'ss':
                return num + (isFuture || withoutSuffix) ? ' másodperc' : ' másodperce';
            case 'm':
                return 'egy' + (isFuture || withoutSuffix ? ' perc' : ' perce');
            case 'mm':
                return num + (isFuture || withoutSuffix ? ' perc' : ' perce');
            case 'h':
                return 'egy' + (isFuture || withoutSuffix ? ' óra' : ' órája');
            case 'hh':
                return num + (isFuture || withoutSuffix ? ' óra' : ' órája');
            case 'd':
                return 'egy' + (isFuture || withoutSuffix ? ' nap' : ' napja');
            case 'dd':
                return num + (isFuture || withoutSuffix ? ' nap' : ' napja');
            case 'M':
                return 'egy' + (isFuture || withoutSuffix ? ' hónap' : ' hónapja');
            case 'MM':
                return num + (isFuture || withoutSuffix ? ' hónap' : ' hónapja');
            case 'y':
                return 'egy' + (isFuture || withoutSuffix ? ' év' : ' éve');
            case 'yy':
                return num + (isFuture || withoutSuffix ? ' év' : ' éve');
        }
        return '';
    }
    function week(isFuture) {
        return (isFuture ? '' : '[múlt] ') + '[' + weekEndings[this.day()] + '] LT[-kor]';
    }

    hooks.defineLocale('hu', {
        months : 'január_február_március_április_május_június_július_augusztus_szeptember_október_november_december'.split('_'),
        monthsShort : 'jan_feb_márc_ápr_máj_jún_júl_aug_szept_okt_nov_dec'.split('_'),
        weekdays : 'vasárnap_hétfő_kedd_szerda_csütörtök_péntek_szombat'.split('_'),
        weekdaysShort : 'vas_hét_kedd_sze_csüt_pén_szo'.split('_'),
        weekdaysMin : 'v_h_k_sze_cs_p_szo'.split('_'),
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'YYYY.MM.DD.',
            LL : 'YYYY. MMMM D.',
            LLL : 'YYYY. MMMM D. H:mm',
            LLLL : 'YYYY. MMMM D., dddd H:mm'
        },
        meridiemParse: /de|du/i,
        isPM: function (input) {
            return input.charAt(1).toLowerCase() === 'u';
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 12) {
                return isLower === true ? 'de' : 'DE';
            } else {
                return isLower === true ? 'du' : 'DU';
            }
        },
        calendar : {
            sameDay : '[ma] LT[-kor]',
            nextDay : '[holnap] LT[-kor]',
            nextWeek : function () {
                return week.call(this, true);
            },
            lastDay : '[tegnap] LT[-kor]',
            lastWeek : function () {
                return week.call(this, false);
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s múlva',
            past : '%s',
            s : translate$4,
            ss : translate$4,
            m : translate$4,
            mm : translate$4,
            h : translate$4,
            hh : translate$4,
            d : translate$4,
            dd : translate$4,
            M : translate$4,
            MM : translate$4,
            y : translate$4,
            yy : translate$4
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('hy-am', {
        months : {
            format: 'հունվարի_փետրվարի_մարտի_ապրիլի_մայիսի_հունիսի_հուլիսի_օգոստոսի_սեպտեմբերի_հոկտեմբերի_նոյեմբերի_դեկտեմբերի'.split('_'),
            standalone: 'հունվար_փետրվար_մարտ_ապրիլ_մայիս_հունիս_հուլիս_օգոստոս_սեպտեմբեր_հոկտեմբեր_նոյեմբեր_դեկտեմբեր'.split('_')
        },
        monthsShort : 'հնվ_փտր_մրտ_ապր_մյս_հնս_հլս_օգս_սպտ_հկտ_նմբ_դկտ'.split('_'),
        weekdays : 'կիրակի_երկուշաբթի_երեքշաբթի_չորեքշաբթի_հինգշաբթի_ուրբաթ_շաբաթ'.split('_'),
        weekdaysShort : 'կրկ_երկ_երք_չրք_հնգ_ուրբ_շբթ'.split('_'),
        weekdaysMin : 'կրկ_երկ_երք_չրք_հնգ_ուրբ_շբթ'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY թ.',
            LLL : 'D MMMM YYYY թ., HH:mm',
            LLLL : 'dddd, D MMMM YYYY թ., HH:mm'
        },
        calendar : {
            sameDay: '[այսօր] LT',
            nextDay: '[վաղը] LT',
            lastDay: '[երեկ] LT',
            nextWeek: function () {
                return 'dddd [օրը ժամը] LT';
            },
            lastWeek: function () {
                return '[անցած] dddd [օրը ժամը] LT';
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : '%s հետո',
            past : '%s առաջ',
            s : 'մի քանի վայրկյան',
            ss : '%d վայրկյան',
            m : 'րոպե',
            mm : '%d րոպե',
            h : 'ժամ',
            hh : '%d ժամ',
            d : 'օր',
            dd : '%d օր',
            M : 'ամիս',
            MM : '%d ամիս',
            y : 'տարի',
            yy : '%d տարի'
        },
        meridiemParse: /գիշերվա|առավոտվա|ցերեկվա|երեկոյան/,
        isPM: function (input) {
            return /^(ցերեկվա|երեկոյան)$/.test(input);
        },
        meridiem : function (hour) {
            if (hour < 4) {
                return 'գիշերվա';
            } else if (hour < 12) {
                return 'առավոտվա';
            } else if (hour < 17) {
                return 'ցերեկվա';
            } else {
                return 'երեկոյան';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}|\d{1,2}-(ին|րդ)/,
        ordinal: function (number, period) {
            switch (period) {
                case 'DDD':
                case 'w':
                case 'W':
                case 'DDDo':
                    if (number === 1) {
                        return number + '-ին';
                    }
                    return number + '-րդ';
                default:
                    return number;
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('id', {
        months : 'Januari_Februari_Maret_April_Mei_Juni_Juli_Agustus_September_Oktober_November_Desember'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_Mei_Jun_Jul_Agt_Sep_Okt_Nov_Des'.split('_'),
        weekdays : 'Minggu_Senin_Selasa_Rabu_Kamis_Jumat_Sabtu'.split('_'),
        weekdaysShort : 'Min_Sen_Sel_Rab_Kam_Jum_Sab'.split('_'),
        weekdaysMin : 'Mg_Sn_Sl_Rb_Km_Jm_Sb'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY [pukul] HH.mm',
            LLLL : 'dddd, D MMMM YYYY [pukul] HH.mm'
        },
        meridiemParse: /pagi|siang|sore|malam/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'pagi') {
                return hour;
            } else if (meridiem === 'siang') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'sore' || meridiem === 'malam') {
                return hour + 12;
            }
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 11) {
                return 'pagi';
            } else if (hours < 15) {
                return 'siang';
            } else if (hours < 19) {
                return 'sore';
            } else {
                return 'malam';
            }
        },
        calendar : {
            sameDay : '[Hari ini pukul] LT',
            nextDay : '[Besok pukul] LT',
            nextWeek : 'dddd [pukul] LT',
            lastDay : '[Kemarin pukul] LT',
            lastWeek : 'dddd [lalu pukul] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dalam %s',
            past : '%s yang lalu',
            s : 'beberapa detik',
            ss : '%d detik',
            m : 'semenit',
            mm : '%d menit',
            h : 'sejam',
            hh : '%d jam',
            d : 'sehari',
            dd : '%d hari',
            M : 'sebulan',
            MM : '%d bulan',
            y : 'setahun',
            yy : '%d tahun'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function plural$2(n) {
        if (n % 100 === 11) {
            return true;
        } else if (n % 10 === 1) {
            return false;
        }
        return true;
    }
    function translate$5(number, withoutSuffix, key, isFuture) {
        var result = number + ' ';
        switch (key) {
            case 's':
                return withoutSuffix || isFuture ? 'nokkrar sekúndur' : 'nokkrum sekúndum';
            case 'ss':
                if (plural$2(number)) {
                    return result + (withoutSuffix || isFuture ? 'sekúndur' : 'sekúndum');
                }
                return result + 'sekúnda';
            case 'm':
                return withoutSuffix ? 'mínúta' : 'mínútu';
            case 'mm':
                if (plural$2(number)) {
                    return result + (withoutSuffix || isFuture ? 'mínútur' : 'mínútum');
                } else if (withoutSuffix) {
                    return result + 'mínúta';
                }
                return result + 'mínútu';
            case 'hh':
                if (plural$2(number)) {
                    return result + (withoutSuffix || isFuture ? 'klukkustundir' : 'klukkustundum');
                }
                return result + 'klukkustund';
            case 'd':
                if (withoutSuffix) {
                    return 'dagur';
                }
                return isFuture ? 'dag' : 'degi';
            case 'dd':
                if (plural$2(number)) {
                    if (withoutSuffix) {
                        return result + 'dagar';
                    }
                    return result + (isFuture ? 'daga' : 'dögum');
                } else if (withoutSuffix) {
                    return result + 'dagur';
                }
                return result + (isFuture ? 'dag' : 'degi');
            case 'M':
                if (withoutSuffix) {
                    return 'mánuður';
                }
                return isFuture ? 'mánuð' : 'mánuði';
            case 'MM':
                if (plural$2(number)) {
                    if (withoutSuffix) {
                        return result + 'mánuðir';
                    }
                    return result + (isFuture ? 'mánuði' : 'mánuðum');
                } else if (withoutSuffix) {
                    return result + 'mánuður';
                }
                return result + (isFuture ? 'mánuð' : 'mánuði');
            case 'y':
                return withoutSuffix || isFuture ? 'ár' : 'ári';
            case 'yy':
                if (plural$2(number)) {
                    return result + (withoutSuffix || isFuture ? 'ár' : 'árum');
                }
                return result + (withoutSuffix || isFuture ? 'ár' : 'ári');
        }
    }

    hooks.defineLocale('is', {
        months : 'janúar_febrúar_mars_apríl_maí_júní_júlí_ágúst_september_október_nóvember_desember'.split('_'),
        monthsShort : 'jan_feb_mar_apr_maí_jún_júl_ágú_sep_okt_nóv_des'.split('_'),
        weekdays : 'sunnudagur_mánudagur_þriðjudagur_miðvikudagur_fimmtudagur_föstudagur_laugardagur'.split('_'),
        weekdaysShort : 'sun_mán_þri_mið_fim_fös_lau'.split('_'),
        weekdaysMin : 'Su_Má_Þr_Mi_Fi_Fö_La'.split('_'),
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY [kl.] H:mm',
            LLLL : 'dddd, D. MMMM YYYY [kl.] H:mm'
        },
        calendar : {
            sameDay : '[í dag kl.] LT',
            nextDay : '[á morgun kl.] LT',
            nextWeek : 'dddd [kl.] LT',
            lastDay : '[í gær kl.] LT',
            lastWeek : '[síðasta] dddd [kl.] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'eftir %s',
            past : 'fyrir %s síðan',
            s : translate$5,
            ss : translate$5,
            m : translate$5,
            mm : translate$5,
            h : 'klukkustund',
            hh : translate$5,
            d : translate$5,
            dd : translate$5,
            M : translate$5,
            MM : translate$5,
            y : translate$5,
            yy : translate$5
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('it-ch', {
        months : 'gennaio_febbraio_marzo_aprile_maggio_giugno_luglio_agosto_settembre_ottobre_novembre_dicembre'.split('_'),
        monthsShort : 'gen_feb_mar_apr_mag_giu_lug_ago_set_ott_nov_dic'.split('_'),
        weekdays : 'domenica_lunedì_martedì_mercoledì_giovedì_venerdì_sabato'.split('_'),
        weekdaysShort : 'dom_lun_mar_mer_gio_ven_sab'.split('_'),
        weekdaysMin : 'do_lu_ma_me_gi_ve_sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Oggi alle] LT',
            nextDay: '[Domani alle] LT',
            nextWeek: 'dddd [alle] LT',
            lastDay: '[Ieri alle] LT',
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[la scorsa] dddd [alle] LT';
                    default:
                        return '[lo scorso] dddd [alle] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : function (s) {
                return ((/^[0-9].+$/).test(s) ? 'tra' : 'in') + ' ' + s;
            },
            past : '%s fa',
            s : 'alcuni secondi',
            ss : '%d secondi',
            m : 'un minuto',
            mm : '%d minuti',
            h : 'un\'ora',
            hh : '%d ore',
            d : 'un giorno',
            dd : '%d giorni',
            M : 'un mese',
            MM : '%d mesi',
            y : 'un anno',
            yy : '%d anni'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal: '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('it', {
        months : 'gennaio_febbraio_marzo_aprile_maggio_giugno_luglio_agosto_settembre_ottobre_novembre_dicembre'.split('_'),
        monthsShort : 'gen_feb_mar_apr_mag_giu_lug_ago_set_ott_nov_dic'.split('_'),
        weekdays : 'domenica_lunedì_martedì_mercoledì_giovedì_venerdì_sabato'.split('_'),
        weekdaysShort : 'dom_lun_mar_mer_gio_ven_sab'.split('_'),
        weekdaysMin : 'do_lu_ma_me_gi_ve_sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Oggi alle] LT',
            nextDay: '[Domani alle] LT',
            nextWeek: 'dddd [alle] LT',
            lastDay: '[Ieri alle] LT',
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[la scorsa] dddd [alle] LT';
                    default:
                        return '[lo scorso] dddd [alle] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : function (s) {
                return ((/^[0-9].+$/).test(s) ? 'tra' : 'in') + ' ' + s;
            },
            past : '%s fa',
            s : 'alcuni secondi',
            ss : '%d secondi',
            m : 'un minuto',
            mm : '%d minuti',
            h : 'un\'ora',
            hh : '%d ore',
            d : 'un giorno',
            dd : '%d giorni',
            M : 'un mese',
            MM : '%d mesi',
            y : 'un anno',
            yy : '%d anni'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal: '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ja', {
        months : '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
        monthsShort : '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
        weekdays : '日曜日_月曜日_火曜日_水曜日_木曜日_金曜日_土曜日'.split('_'),
        weekdaysShort : '日_月_火_水_木_金_土'.split('_'),
        weekdaysMin : '日_月_火_水_木_金_土'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY/MM/DD',
            LL : 'YYYY年M月D日',
            LLL : 'YYYY年M月D日 HH:mm',
            LLLL : 'YYYY年M月D日 dddd HH:mm',
            l : 'YYYY/MM/DD',
            ll : 'YYYY年M月D日',
            lll : 'YYYY年M月D日 HH:mm',
            llll : 'YYYY年M月D日(ddd) HH:mm'
        },
        meridiemParse: /午前|午後/i,
        isPM : function (input) {
            return input === '午後';
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return '午前';
            } else {
                return '午後';
            }
        },
        calendar : {
            sameDay : '[今日] LT',
            nextDay : '[明日] LT',
            nextWeek : function (now) {
                if (now.week() < this.week()) {
                    return '[来週]dddd LT';
                } else {
                    return 'dddd LT';
                }
            },
            lastDay : '[昨日] LT',
            lastWeek : function (now) {
                if (this.week() < now.week()) {
                    return '[先週]dddd LT';
                } else {
                    return 'dddd LT';
                }
            },
            sameElse : 'L'
        },
        dayOfMonthOrdinalParse : /\d{1,2}日/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'DDD':
                    return number + '日';
                default:
                    return number;
            }
        },
        relativeTime : {
            future : '%s後',
            past : '%s前',
            s : '数秒',
            ss : '%d秒',
            m : '1分',
            mm : '%d分',
            h : '1時間',
            hh : '%d時間',
            d : '1日',
            dd : '%d日',
            M : '1ヶ月',
            MM : '%dヶ月',
            y : '1年',
            yy : '%d年'
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('jv', {
        months : 'Januari_Februari_Maret_April_Mei_Juni_Juli_Agustus_September_Oktober_Nopember_Desember'.split('_'),
        monthsShort : 'Jan_Feb_Mar_Apr_Mei_Jun_Jul_Ags_Sep_Okt_Nop_Des'.split('_'),
        weekdays : 'Minggu_Senen_Seloso_Rebu_Kemis_Jemuwah_Septu'.split('_'),
        weekdaysShort : 'Min_Sen_Sel_Reb_Kem_Jem_Sep'.split('_'),
        weekdaysMin : 'Mg_Sn_Sl_Rb_Km_Jm_Sp'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY [pukul] HH.mm',
            LLLL : 'dddd, D MMMM YYYY [pukul] HH.mm'
        },
        meridiemParse: /enjing|siyang|sonten|ndalu/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'enjing') {
                return hour;
            } else if (meridiem === 'siyang') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'sonten' || meridiem === 'ndalu') {
                return hour + 12;
            }
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 11) {
                return 'enjing';
            } else if (hours < 15) {
                return 'siyang';
            } else if (hours < 19) {
                return 'sonten';
            } else {
                return 'ndalu';
            }
        },
        calendar : {
            sameDay : '[Dinten puniko pukul] LT',
            nextDay : '[Mbenjang pukul] LT',
            nextWeek : 'dddd [pukul] LT',
            lastDay : '[Kala wingi pukul] LT',
            lastWeek : 'dddd [kepengker pukul] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'wonten ing %s',
            past : '%s ingkang kepengker',
            s : 'sawetawis detik',
            ss : '%d detik',
            m : 'setunggal menit',
            mm : '%d menit',
            h : 'setunggal jam',
            hh : '%d jam',
            d : 'sedinten',
            dd : '%d dinten',
            M : 'sewulan',
            MM : '%d wulan',
            y : 'setaun',
            yy : '%d taun'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ka', {
        months : {
            standalone: 'იანვარი_თებერვალი_მარტი_აპრილი_მაისი_ივნისი_ივლისი_აგვისტო_სექტემბერი_ოქტომბერი_ნოემბერი_დეკემბერი'.split('_'),
            format: 'იანვარს_თებერვალს_მარტს_აპრილის_მაისს_ივნისს_ივლისს_აგვისტს_სექტემბერს_ოქტომბერს_ნოემბერს_დეკემბერს'.split('_')
        },
        monthsShort : 'იან_თებ_მარ_აპრ_მაი_ივნ_ივლ_აგვ_სექ_ოქტ_ნოე_დეკ'.split('_'),
        weekdays : {
            standalone: 'კვირა_ორშაბათი_სამშაბათი_ოთხშაბათი_ხუთშაბათი_პარასკევი_შაბათი'.split('_'),
            format: 'კვირას_ორშაბათს_სამშაბათს_ოთხშაბათს_ხუთშაბათს_პარასკევს_შაბათს'.split('_'),
            isFormat: /(წინა|შემდეგ)/
        },
        weekdaysShort : 'კვი_ორშ_სამ_ოთხ_ხუთ_პარ_შაბ'.split('_'),
        weekdaysMin : 'კვ_ორ_სა_ოთ_ხუ_პა_შა'.split('_'),
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendar : {
            sameDay : '[დღეს] LT[-ზე]',
            nextDay : '[ხვალ] LT[-ზე]',
            lastDay : '[გუშინ] LT[-ზე]',
            nextWeek : '[შემდეგ] dddd LT[-ზე]',
            lastWeek : '[წინა] dddd LT-ზე',
            sameElse : 'L'
        },
        relativeTime : {
            future : function (s) {
                return (/(წამი|წუთი|საათი|წელი)/).test(s) ?
                    s.replace(/ი$/, 'ში') :
                    s + 'ში';
            },
            past : function (s) {
                if ((/(წამი|წუთი|საათი|დღე|თვე)/).test(s)) {
                    return s.replace(/(ი|ე)$/, 'ის წინ');
                }
                if ((/წელი/).test(s)) {
                    return s.replace(/წელი$/, 'წლის წინ');
                }
            },
            s : 'რამდენიმე წამი',
            ss : '%d წამი',
            m : 'წუთი',
            mm : '%d წუთი',
            h : 'საათი',
            hh : '%d საათი',
            d : 'დღე',
            dd : '%d დღე',
            M : 'თვე',
            MM : '%d თვე',
            y : 'წელი',
            yy : '%d წელი'
        },
        dayOfMonthOrdinalParse: /0|1-ლი|მე-\d{1,2}|\d{1,2}-ე/,
        ordinal : function (number) {
            if (number === 0) {
                return number;
            }
            if (number === 1) {
                return number + '-ლი';
            }
            if ((number < 20) || (number <= 100 && (number % 20 === 0)) || (number % 100 === 0)) {
                return 'მე-' + number;
            }
            return number + '-ე';
        },
        week : {
            dow : 1,
            doy : 7
        }
    });

    //! moment.js locale configuration

    var suffixes$1 = {
        0: '-ші',
        1: '-ші',
        2: '-ші',
        3: '-ші',
        4: '-ші',
        5: '-ші',
        6: '-шы',
        7: '-ші',
        8: '-ші',
        9: '-шы',
        10: '-шы',
        20: '-шы',
        30: '-шы',
        40: '-шы',
        50: '-ші',
        60: '-шы',
        70: '-ші',
        80: '-ші',
        90: '-шы',
        100: '-ші'
    };

    hooks.defineLocale('kk', {
        months : 'қаңтар_ақпан_наурыз_сәуір_мамыр_маусым_шілде_тамыз_қыркүйек_қазан_қараша_желтоқсан'.split('_'),
        monthsShort : 'қаң_ақп_нау_сәу_мам_мау_шіл_там_қыр_қаз_қар_жел'.split('_'),
        weekdays : 'жексенбі_дүйсенбі_сейсенбі_сәрсенбі_бейсенбі_жұма_сенбі'.split('_'),
        weekdaysShort : 'жек_дүй_сей_сәр_бей_жұм_сен'.split('_'),
        weekdaysMin : 'жк_дй_сй_ср_бй_жм_сн'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Бүгін сағат] LT',
            nextDay : '[Ертең сағат] LT',
            nextWeek : 'dddd [сағат] LT',
            lastDay : '[Кеше сағат] LT',
            lastWeek : '[Өткен аптаның] dddd [сағат] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s ішінде',
            past : '%s бұрын',
            s : 'бірнеше секунд',
            ss : '%d секунд',
            m : 'бір минут',
            mm : '%d минут',
            h : 'бір сағат',
            hh : '%d сағат',
            d : 'бір күн',
            dd : '%d күн',
            M : 'бір ай',
            MM : '%d ай',
            y : 'бір жыл',
            yy : '%d жыл'
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(ші|шы)/,
        ordinal : function (number) {
            var a = number % 10,
                b = number >= 100 ? 100 : null;
            return number + (suffixes$1[number] || suffixes$1[a] || suffixes$1[b]);
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$8 = {
        '1': '១',
        '2': '២',
        '3': '៣',
        '4': '៤',
        '5': '៥',
        '6': '៦',
        '7': '៧',
        '8': '៨',
        '9': '៩',
        '0': '០'
    }, numberMap$7 = {
        '១': '1',
        '២': '2',
        '៣': '3',
        '៤': '4',
        '៥': '5',
        '៦': '6',
        '៧': '7',
        '៨': '8',
        '៩': '9',
        '០': '0'
    };

    hooks.defineLocale('km', {
        months: 'មករា_កុម្ភៈ_មីនា_មេសា_ឧសភា_មិថុនា_កក្កដា_សីហា_កញ្ញា_តុលា_វិច្ឆិកា_ធ្នូ'.split(
            '_'
        ),
        monthsShort: 'មករា_កុម្ភៈ_មីនា_មេសា_ឧសភា_មិថុនា_កក្កដា_សីហា_កញ្ញា_តុលា_វិច្ឆិកា_ធ្នូ'.split(
            '_'
        ),
        weekdays: 'អាទិត្យ_ច័ន្ទ_អង្គារ_ពុធ_ព្រហស្បតិ៍_សុក្រ_សៅរ៍'.split('_'),
        weekdaysShort: 'អា_ច_អ_ព_ព្រ_សុ_ស'.split('_'),
        weekdaysMin: 'អា_ច_អ_ព_ព្រ_សុ_ស'.split('_'),
        weekdaysParseExact: true,
        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY HH:mm',
            LLLL: 'dddd, D MMMM YYYY HH:mm'
        },
        meridiemParse: /ព្រឹក|ល្ងាច/,
        isPM: function (input) {
            return input === 'ល្ងាច';
        },
        meridiem: function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ព្រឹក';
            } else {
                return 'ល្ងាច';
            }
        },
        calendar: {
            sameDay: '[ថ្ងៃនេះ ម៉ោង] LT',
            nextDay: '[ស្អែក ម៉ោង] LT',
            nextWeek: 'dddd [ម៉ោង] LT',
            lastDay: '[ម្សិលមិញ ម៉ោង] LT',
            lastWeek: 'dddd [សប្តាហ៍មុន] [ម៉ោង] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: '%sទៀត',
            past: '%sមុន',
            s: 'ប៉ុន្មានវិនាទី',
            ss: '%d វិនាទី',
            m: 'មួយនាទី',
            mm: '%d នាទី',
            h: 'មួយម៉ោង',
            hh: '%d ម៉ោង',
            d: 'មួយថ្ងៃ',
            dd: '%d ថ្ងៃ',
            M: 'មួយខែ',
            MM: '%d ខែ',
            y: 'មួយឆ្នាំ',
            yy: '%d ឆ្នាំ'
        },
        dayOfMonthOrdinalParse : /ទី\d{1,2}/,
        ordinal : 'ទី%d',
        preparse: function (string) {
            return string.replace(/[១២៣៤៥៦៧៨៩០]/g, function (match) {
                return numberMap$7[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$8[match];
            });
        },
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4 // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$9 = {
        '1': '೧',
        '2': '೨',
        '3': '೩',
        '4': '೪',
        '5': '೫',
        '6': '೬',
        '7': '೭',
        '8': '೮',
        '9': '೯',
        '0': '೦'
    },
    numberMap$8 = {
        '೧': '1',
        '೨': '2',
        '೩': '3',
        '೪': '4',
        '೫': '5',
        '೬': '6',
        '೭': '7',
        '೮': '8',
        '೯': '9',
        '೦': '0'
    };

    hooks.defineLocale('kn', {
        months : 'ಜನವರಿ_ಫೆಬ್ರವರಿ_ಮಾರ್ಚ್_ಏಪ್ರಿಲ್_ಮೇ_ಜೂನ್_ಜುಲೈ_ಆಗಸ್ಟ್_ಸೆಪ್ಟೆಂಬರ್_ಅಕ್ಟೋಬರ್_ನವೆಂಬರ್_ಡಿಸೆಂಬರ್'.split('_'),
        monthsShort : 'ಜನ_ಫೆಬ್ರ_ಮಾರ್ಚ್_ಏಪ್ರಿಲ್_ಮೇ_ಜೂನ್_ಜುಲೈ_ಆಗಸ್ಟ್_ಸೆಪ್ಟೆಂ_ಅಕ್ಟೋ_ನವೆಂ_ಡಿಸೆಂ'.split('_'),
        monthsParseExact: true,
        weekdays : 'ಭಾನುವಾರ_ಸೋಮವಾರ_ಮಂಗಳವಾರ_ಬುಧವಾರ_ಗುರುವಾರ_ಶುಕ್ರವಾರ_ಶನಿವಾರ'.split('_'),
        weekdaysShort : 'ಭಾನು_ಸೋಮ_ಮಂಗಳ_ಬುಧ_ಗುರು_ಶುಕ್ರ_ಶನಿ'.split('_'),
        weekdaysMin : 'ಭಾ_ಸೋ_ಮಂ_ಬು_ಗು_ಶು_ಶ'.split('_'),
        longDateFormat : {
            LT : 'A h:mm',
            LTS : 'A h:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm',
            LLLL : 'dddd, D MMMM YYYY, A h:mm'
        },
        calendar : {
            sameDay : '[ಇಂದು] LT',
            nextDay : '[ನಾಳೆ] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[ನಿನ್ನೆ] LT',
            lastWeek : '[ಕೊನೆಯ] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s ನಂತರ',
            past : '%s ಹಿಂದೆ',
            s : 'ಕೆಲವು ಕ್ಷಣಗಳು',
            ss : '%d ಸೆಕೆಂಡುಗಳು',
            m : 'ಒಂದು ನಿಮಿಷ',
            mm : '%d ನಿಮಿಷ',
            h : 'ಒಂದು ಗಂಟೆ',
            hh : '%d ಗಂಟೆ',
            d : 'ಒಂದು ದಿನ',
            dd : '%d ದಿನ',
            M : 'ಒಂದು ತಿಂಗಳು',
            MM : '%d ತಿಂಗಳು',
            y : 'ಒಂದು ವರ್ಷ',
            yy : '%d ವರ್ಷ'
        },
        preparse: function (string) {
            return string.replace(/[೧೨೩೪೫೬೭೮೯೦]/g, function (match) {
                return numberMap$8[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$9[match];
            });
        },
        meridiemParse: /ರಾತ್ರಿ|ಬೆಳಿಗ್ಗೆ|ಮಧ್ಯಾಹ್ನ|ಸಂಜೆ/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'ರಾತ್ರಿ') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'ಬೆಳಿಗ್ಗೆ') {
                return hour;
            } else if (meridiem === 'ಮಧ್ಯಾಹ್ನ') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'ಸಂಜೆ') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'ರಾತ್ರಿ';
            } else if (hour < 10) {
                return 'ಬೆಳಿಗ್ಗೆ';
            } else if (hour < 17) {
                return 'ಮಧ್ಯಾಹ್ನ';
            } else if (hour < 20) {
                return 'ಸಂಜೆ';
            } else {
                return 'ರಾತ್ರಿ';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}(ನೇ)/,
        ordinal : function (number) {
            return number + 'ನೇ';
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ko', {
        months : '1월_2월_3월_4월_5월_6월_7월_8월_9월_10월_11월_12월'.split('_'),
        monthsShort : '1월_2월_3월_4월_5월_6월_7월_8월_9월_10월_11월_12월'.split('_'),
        weekdays : '일요일_월요일_화요일_수요일_목요일_금요일_토요일'.split('_'),
        weekdaysShort : '일_월_화_수_목_금_토'.split('_'),
        weekdaysMin : '일_월_화_수_목_금_토'.split('_'),
        longDateFormat : {
            LT : 'A h:mm',
            LTS : 'A h:mm:ss',
            L : 'YYYY.MM.DD.',
            LL : 'YYYY년 MMMM D일',
            LLL : 'YYYY년 MMMM D일 A h:mm',
            LLLL : 'YYYY년 MMMM D일 dddd A h:mm',
            l : 'YYYY.MM.DD.',
            ll : 'YYYY년 MMMM D일',
            lll : 'YYYY년 MMMM D일 A h:mm',
            llll : 'YYYY년 MMMM D일 dddd A h:mm'
        },
        calendar : {
            sameDay : '오늘 LT',
            nextDay : '내일 LT',
            nextWeek : 'dddd LT',
            lastDay : '어제 LT',
            lastWeek : '지난주 dddd LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s 후',
            past : '%s 전',
            s : '몇 초',
            ss : '%d초',
            m : '1분',
            mm : '%d분',
            h : '한 시간',
            hh : '%d시간',
            d : '하루',
            dd : '%d일',
            M : '한 달',
            MM : '%d달',
            y : '일 년',
            yy : '%d년'
        },
        dayOfMonthOrdinalParse : /\d{1,2}(일|월|주)/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'DDD':
                    return number + '일';
                case 'M':
                    return number + '월';
                case 'w':
                case 'W':
                    return number + '주';
                default:
                    return number;
            }
        },
        meridiemParse : /오전|오후/,
        isPM : function (token) {
            return token === '오후';
        },
        meridiem : function (hour, minute, isUpper) {
            return hour < 12 ? '오전' : '오후';
        }
    });

    //! moment.js locale configuration

    var symbolMap$a = {
        '1': '١',
        '2': '٢',
        '3': '٣',
        '4': '٤',
        '5': '٥',
        '6': '٦',
        '7': '٧',
        '8': '٨',
        '9': '٩',
        '0': '٠'
    }, numberMap$9 = {
        '١': '1',
        '٢': '2',
        '٣': '3',
        '٤': '4',
        '٥': '5',
        '٦': '6',
        '٧': '7',
        '٨': '8',
        '٩': '9',
        '٠': '0'
    },
    months$7 = [
        'کانونی دووەم',
        'شوبات',
        'ئازار',
        'نیسان',
        'ئایار',
        'حوزەیران',
        'تەمموز',
        'ئاب',
        'ئەیلوول',
        'تشرینی یەكەم',
        'تشرینی دووەم',
        'كانونی یەکەم'
    ];


    hooks.defineLocale('ku', {
        months : months$7,
        monthsShort : months$7,
        weekdays : 'یه‌كشه‌ممه‌_دووشه‌ممه‌_سێشه‌ممه‌_چوارشه‌ممه‌_پێنجشه‌ممه‌_هه‌ینی_شه‌ممه‌'.split('_'),
        weekdaysShort : 'یه‌كشه‌م_دووشه‌م_سێشه‌م_چوارشه‌م_پێنجشه‌م_هه‌ینی_شه‌ممه‌'.split('_'),
        weekdaysMin : 'ی_د_س_چ_پ_ه_ش'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        meridiemParse: /ئێواره‌|به‌یانی/,
        isPM: function (input) {
            return /ئێواره‌/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'به‌یانی';
            } else {
                return 'ئێواره‌';
            }
        },
        calendar : {
            sameDay : '[ئه‌مرۆ كاتژمێر] LT',
            nextDay : '[به‌یانی كاتژمێر] LT',
            nextWeek : 'dddd [كاتژمێر] LT',
            lastDay : '[دوێنێ كاتژمێر] LT',
            lastWeek : 'dddd [كاتژمێر] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'له‌ %s',
            past : '%s',
            s : 'چه‌ند چركه‌یه‌ك',
            ss : 'چركه‌ %d',
            m : 'یه‌ك خوله‌ك',
            mm : '%d خوله‌ك',
            h : 'یه‌ك كاتژمێر',
            hh : '%d كاتژمێر',
            d : 'یه‌ك ڕۆژ',
            dd : '%d ڕۆژ',
            M : 'یه‌ك مانگ',
            MM : '%d مانگ',
            y : 'یه‌ك ساڵ',
            yy : '%d ساڵ'
        },
        preparse: function (string) {
            return string.replace(/[١٢٣٤٥٦٧٨٩٠]/g, function (match) {
                return numberMap$9[match];
            }).replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$a[match];
            }).replace(/,/g, '،');
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12 // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var suffixes$2 = {
        0: '-чү',
        1: '-чи',
        2: '-чи',
        3: '-чү',
        4: '-чү',
        5: '-чи',
        6: '-чы',
        7: '-чи',
        8: '-чи',
        9: '-чу',
        10: '-чу',
        20: '-чы',
        30: '-чу',
        40: '-чы',
        50: '-чү',
        60: '-чы',
        70: '-чи',
        80: '-чи',
        90: '-чу',
        100: '-чү'
    };

    hooks.defineLocale('ky', {
        months : 'январь_февраль_март_апрель_май_июнь_июль_август_сентябрь_октябрь_ноябрь_декабрь'.split('_'),
        monthsShort : 'янв_фев_март_апр_май_июнь_июль_авг_сен_окт_ноя_дек'.split('_'),
        weekdays : 'Жекшемби_Дүйшөмбү_Шейшемби_Шаршемби_Бейшемби_Жума_Ишемби'.split('_'),
        weekdaysShort : 'Жек_Дүй_Шей_Шар_Бей_Жум_Ише'.split('_'),
        weekdaysMin : 'Жк_Дй_Шй_Шр_Бй_Жм_Иш'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Бүгүн саат] LT',
            nextDay : '[Эртең саат] LT',
            nextWeek : 'dddd [саат] LT',
            lastDay : '[Кечээ саат] LT',
            lastWeek : '[Өткөн аптанын] dddd [күнү] [саат] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s ичинде',
            past : '%s мурун',
            s : 'бирнече секунд',
            ss : '%d секунд',
            m : 'бир мүнөт',
            mm : '%d мүнөт',
            h : 'бир саат',
            hh : '%d саат',
            d : 'бир күн',
            dd : '%d күн',
            M : 'бир ай',
            MM : '%d ай',
            y : 'бир жыл',
            yy : '%d жыл'
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(чи|чы|чү|чу)/,
        ordinal : function (number) {
            var a = number % 10,
                b = number >= 100 ? 100 : null;
            return number + (suffixes$2[number] || suffixes$2[a] || suffixes$2[b]);
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$5(number, withoutSuffix, key, isFuture) {
        var format = {
            'm': ['eng Minutt', 'enger Minutt'],
            'h': ['eng Stonn', 'enger Stonn'],
            'd': ['een Dag', 'engem Dag'],
            'M': ['ee Mount', 'engem Mount'],
            'y': ['ee Joer', 'engem Joer']
        };
        return withoutSuffix ? format[key][0] : format[key][1];
    }
    function processFutureTime(string) {
        var number = string.substr(0, string.indexOf(' '));
        if (eifelerRegelAppliesToNumber(number)) {
            return 'a ' + string;
        }
        return 'an ' + string;
    }
    function processPastTime(string) {
        var number = string.substr(0, string.indexOf(' '));
        if (eifelerRegelAppliesToNumber(number)) {
            return 'viru ' + string;
        }
        return 'virun ' + string;
    }
    /**
     * Returns true if the word before the given number loses the '-n' ending.
     * e.g. 'an 10 Deeg' but 'a 5 Deeg'
     *
     * @param number {integer}
     * @returns {boolean}
     */
    function eifelerRegelAppliesToNumber(number) {
        number = parseInt(number, 10);
        if (isNaN(number)) {
            return false;
        }
        if (number < 0) {
            // Negative Number --> always true
            return true;
        } else if (number < 10) {
            // Only 1 digit
            if (4 <= number && number <= 7) {
                return true;
            }
            return false;
        } else if (number < 100) {
            // 2 digits
            var lastDigit = number % 10, firstDigit = number / 10;
            if (lastDigit === 0) {
                return eifelerRegelAppliesToNumber(firstDigit);
            }
            return eifelerRegelAppliesToNumber(lastDigit);
        } else if (number < 10000) {
            // 3 or 4 digits --> recursively check first digit
            while (number >= 10) {
                number = number / 10;
            }
            return eifelerRegelAppliesToNumber(number);
        } else {
            // Anything larger than 4 digits: recursively check first n-3 digits
            number = number / 1000;
            return eifelerRegelAppliesToNumber(number);
        }
    }

    hooks.defineLocale('lb', {
        months: 'Januar_Februar_Mäerz_Abrëll_Mee_Juni_Juli_August_September_Oktober_November_Dezember'.split('_'),
        monthsShort: 'Jan._Febr._Mrz._Abr._Mee_Jun._Jul._Aug._Sept._Okt._Nov._Dez.'.split('_'),
        monthsParseExact : true,
        weekdays: 'Sonndeg_Méindeg_Dënschdeg_Mëttwoch_Donneschdeg_Freideg_Samschdeg'.split('_'),
        weekdaysShort: 'So._Mé._Dë._Më._Do._Fr._Sa.'.split('_'),
        weekdaysMin: 'So_Mé_Dë_Më_Do_Fr_Sa'.split('_'),
        weekdaysParseExact : true,
        longDateFormat: {
            LT: 'H:mm [Auer]',
            LTS: 'H:mm:ss [Auer]',
            L: 'DD.MM.YYYY',
            LL: 'D. MMMM YYYY',
            LLL: 'D. MMMM YYYY H:mm [Auer]',
            LLLL: 'dddd, D. MMMM YYYY H:mm [Auer]'
        },
        calendar: {
            sameDay: '[Haut um] LT',
            sameElse: 'L',
            nextDay: '[Muer um] LT',
            nextWeek: 'dddd [um] LT',
            lastDay: '[Gëschter um] LT',
            lastWeek: function () {
                // Different date string for 'Dënschdeg' (Tuesday) and 'Donneschdeg' (Thursday) due to phonological rule
                switch (this.day()) {
                    case 2:
                    case 4:
                        return '[Leschten] dddd [um] LT';
                    default:
                        return '[Leschte] dddd [um] LT';
                }
            }
        },
        relativeTime : {
            future : processFutureTime,
            past : processPastTime,
            s : 'e puer Sekonnen',
            ss : '%d Sekonnen',
            m : processRelativeTime$5,
            mm : '%d Minutten',
            h : processRelativeTime$5,
            hh : '%d Stonnen',
            d : processRelativeTime$5,
            dd : '%d Deeg',
            M : processRelativeTime$5,
            MM : '%d Méint',
            y : processRelativeTime$5,
            yy : '%d Joer'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal: '%d.',
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('lo', {
        months : 'ມັງກອນ_ກຸມພາ_ມີນາ_ເມສາ_ພຶດສະພາ_ມິຖຸນາ_ກໍລະກົດ_ສິງຫາ_ກັນຍາ_ຕຸລາ_ພະຈິກ_ທັນວາ'.split('_'),
        monthsShort : 'ມັງກອນ_ກຸມພາ_ມີນາ_ເມສາ_ພຶດສະພາ_ມິຖຸນາ_ກໍລະກົດ_ສິງຫາ_ກັນຍາ_ຕຸລາ_ພະຈິກ_ທັນວາ'.split('_'),
        weekdays : 'ອາທິດ_ຈັນ_ອັງຄານ_ພຸດ_ພະຫັດ_ສຸກ_ເສົາ'.split('_'),
        weekdaysShort : 'ທິດ_ຈັນ_ອັງຄານ_ພຸດ_ພະຫັດ_ສຸກ_ເສົາ'.split('_'),
        weekdaysMin : 'ທ_ຈ_ອຄ_ພ_ພຫ_ສກ_ສ'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'ວັນdddd D MMMM YYYY HH:mm'
        },
        meridiemParse: /ຕອນເຊົ້າ|ຕອນແລງ/,
        isPM: function (input) {
            return input === 'ຕອນແລງ';
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ຕອນເຊົ້າ';
            } else {
                return 'ຕອນແລງ';
            }
        },
        calendar : {
            sameDay : '[ມື້ນີ້ເວລາ] LT',
            nextDay : '[ມື້ອື່ນເວລາ] LT',
            nextWeek : '[ວັນ]dddd[ໜ້າເວລາ] LT',
            lastDay : '[ມື້ວານນີ້ເວລາ] LT',
            lastWeek : '[ວັນ]dddd[ແລ້ວນີ້ເວລາ] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'ອີກ %s',
            past : '%sຜ່ານມາ',
            s : 'ບໍ່ເທົ່າໃດວິນາທີ',
            ss : '%d ວິນາທີ' ,
            m : '1 ນາທີ',
            mm : '%d ນາທີ',
            h : '1 ຊົ່ວໂມງ',
            hh : '%d ຊົ່ວໂມງ',
            d : '1 ມື້',
            dd : '%d ມື້',
            M : '1 ເດືອນ',
            MM : '%d ເດືອນ',
            y : '1 ປີ',
            yy : '%d ປີ'
        },
        dayOfMonthOrdinalParse: /(ທີ່)\d{1,2}/,
        ordinal : function (number) {
            return 'ທີ່' + number;
        }
    });

    //! moment.js locale configuration

    var units = {
        'ss' : 'sekundė_sekundžių_sekundes',
        'm' : 'minutė_minutės_minutę',
        'mm': 'minutės_minučių_minutes',
        'h' : 'valanda_valandos_valandą',
        'hh': 'valandos_valandų_valandas',
        'd' : 'diena_dienos_dieną',
        'dd': 'dienos_dienų_dienas',
        'M' : 'mėnuo_mėnesio_mėnesį',
        'MM': 'mėnesiai_mėnesių_mėnesius',
        'y' : 'metai_metų_metus',
        'yy': 'metai_metų_metus'
    };
    function translateSeconds(number, withoutSuffix, key, isFuture) {
        if (withoutSuffix) {
            return 'kelios sekundės';
        } else {
            return isFuture ? 'kelių sekundžių' : 'kelias sekundes';
        }
    }
    function translateSingular(number, withoutSuffix, key, isFuture) {
        return withoutSuffix ? forms(key)[0] : (isFuture ? forms(key)[1] : forms(key)[2]);
    }
    function special(number) {
        return number % 10 === 0 || (number > 10 && number < 20);
    }
    function forms(key) {
        return units[key].split('_');
    }
    function translate$6(number, withoutSuffix, key, isFuture) {
        var result = number + ' ';
        if (number === 1) {
            return result + translateSingular(number, withoutSuffix, key[0], isFuture);
        } else if (withoutSuffix) {
            return result + (special(number) ? forms(key)[1] : forms(key)[0]);
        } else {
            if (isFuture) {
                return result + forms(key)[1];
            } else {
                return result + (special(number) ? forms(key)[1] : forms(key)[2]);
            }
        }
    }
    hooks.defineLocale('lt', {
        months : {
            format: 'sausio_vasario_kovo_balandžio_gegužės_birželio_liepos_rugpjūčio_rugsėjo_spalio_lapkričio_gruodžio'.split('_'),
            standalone: 'sausis_vasaris_kovas_balandis_gegužė_birželis_liepa_rugpjūtis_rugsėjis_spalis_lapkritis_gruodis'.split('_'),
            isFormat: /D[oD]?(\[[^\[\]]*\]|\s)+MMMM?|MMMM?(\[[^\[\]]*\]|\s)+D[oD]?/
        },
        monthsShort : 'sau_vas_kov_bal_geg_bir_lie_rgp_rgs_spa_lap_grd'.split('_'),
        weekdays : {
            format: 'sekmadienį_pirmadienį_antradienį_trečiadienį_ketvirtadienį_penktadienį_šeštadienį'.split('_'),
            standalone: 'sekmadienis_pirmadienis_antradienis_trečiadienis_ketvirtadienis_penktadienis_šeštadienis'.split('_'),
            isFormat: /dddd HH:mm/
        },
        weekdaysShort : 'Sek_Pir_Ant_Tre_Ket_Pen_Šeš'.split('_'),
        weekdaysMin : 'S_P_A_T_K_Pn_Š'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'YYYY [m.] MMMM D [d.]',
            LLL : 'YYYY [m.] MMMM D [d.], HH:mm [val.]',
            LLLL : 'YYYY [m.] MMMM D [d.], dddd, HH:mm [val.]',
            l : 'YYYY-MM-DD',
            ll : 'YYYY [m.] MMMM D [d.]',
            lll : 'YYYY [m.] MMMM D [d.], HH:mm [val.]',
            llll : 'YYYY [m.] MMMM D [d.], ddd, HH:mm [val.]'
        },
        calendar : {
            sameDay : '[Šiandien] LT',
            nextDay : '[Rytoj] LT',
            nextWeek : 'dddd LT',
            lastDay : '[Vakar] LT',
            lastWeek : '[Praėjusį] dddd LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'po %s',
            past : 'prieš %s',
            s : translateSeconds,
            ss : translate$6,
            m : translateSingular,
            mm : translate$6,
            h : translateSingular,
            hh : translate$6,
            d : translateSingular,
            dd : translate$6,
            M : translateSingular,
            MM : translate$6,
            y : translateSingular,
            yy : translate$6
        },
        dayOfMonthOrdinalParse: /\d{1,2}-oji/,
        ordinal : function (number) {
            return number + '-oji';
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var units$1 = {
        'ss': 'sekundes_sekundēm_sekunde_sekundes'.split('_'),
        'm': 'minūtes_minūtēm_minūte_minūtes'.split('_'),
        'mm': 'minūtes_minūtēm_minūte_minūtes'.split('_'),
        'h': 'stundas_stundām_stunda_stundas'.split('_'),
        'hh': 'stundas_stundām_stunda_stundas'.split('_'),
        'd': 'dienas_dienām_diena_dienas'.split('_'),
        'dd': 'dienas_dienām_diena_dienas'.split('_'),
        'M': 'mēneša_mēnešiem_mēnesis_mēneši'.split('_'),
        'MM': 'mēneša_mēnešiem_mēnesis_mēneši'.split('_'),
        'y': 'gada_gadiem_gads_gadi'.split('_'),
        'yy': 'gada_gadiem_gads_gadi'.split('_')
    };
    /**
     * @param withoutSuffix boolean true = a length of time; false = before/after a period of time.
     */
    function format$1(forms, number, withoutSuffix) {
        if (withoutSuffix) {
            // E.g. "21 minūte", "3 minūtes".
            return number % 10 === 1 && number % 100 !== 11 ? forms[2] : forms[3];
        } else {
            // E.g. "21 minūtes" as in "pēc 21 minūtes".
            // E.g. "3 minūtēm" as in "pēc 3 minūtēm".
            return number % 10 === 1 && number % 100 !== 11 ? forms[0] : forms[1];
        }
    }
    function relativeTimeWithPlural$1(number, withoutSuffix, key) {
        return number + ' ' + format$1(units$1[key], number, withoutSuffix);
    }
    function relativeTimeWithSingular(number, withoutSuffix, key) {
        return format$1(units$1[key], number, withoutSuffix);
    }
    function relativeSeconds(number, withoutSuffix) {
        return withoutSuffix ? 'dažas sekundes' : 'dažām sekundēm';
    }

    hooks.defineLocale('lv', {
        months : 'janvāris_februāris_marts_aprīlis_maijs_jūnijs_jūlijs_augusts_septembris_oktobris_novembris_decembris'.split('_'),
        monthsShort : 'jan_feb_mar_apr_mai_jūn_jūl_aug_sep_okt_nov_dec'.split('_'),
        weekdays : 'svētdiena_pirmdiena_otrdiena_trešdiena_ceturtdiena_piektdiena_sestdiena'.split('_'),
        weekdaysShort : 'Sv_P_O_T_C_Pk_S'.split('_'),
        weekdaysMin : 'Sv_P_O_T_C_Pk_S'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY.',
            LL : 'YYYY. [gada] D. MMMM',
            LLL : 'YYYY. [gada] D. MMMM, HH:mm',
            LLLL : 'YYYY. [gada] D. MMMM, dddd, HH:mm'
        },
        calendar : {
            sameDay : '[Šodien pulksten] LT',
            nextDay : '[Rīt pulksten] LT',
            nextWeek : 'dddd [pulksten] LT',
            lastDay : '[Vakar pulksten] LT',
            lastWeek : '[Pagājušā] dddd [pulksten] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'pēc %s',
            past : 'pirms %s',
            s : relativeSeconds,
            ss : relativeTimeWithPlural$1,
            m : relativeTimeWithSingular,
            mm : relativeTimeWithPlural$1,
            h : relativeTimeWithSingular,
            hh : relativeTimeWithPlural$1,
            d : relativeTimeWithSingular,
            dd : relativeTimeWithPlural$1,
            M : relativeTimeWithSingular,
            MM : relativeTimeWithPlural$1,
            y : relativeTimeWithSingular,
            yy : relativeTimeWithPlural$1
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var translator = {
        words: { //Different grammatical cases
            ss: ['sekund', 'sekunda', 'sekundi'],
            m: ['jedan minut', 'jednog minuta'],
            mm: ['minut', 'minuta', 'minuta'],
            h: ['jedan sat', 'jednog sata'],
            hh: ['sat', 'sata', 'sati'],
            dd: ['dan', 'dana', 'dana'],
            MM: ['mjesec', 'mjeseca', 'mjeseci'],
            yy: ['godina', 'godine', 'godina']
        },
        correctGrammaticalCase: function (number, wordKey) {
            return number === 1 ? wordKey[0] : (number >= 2 && number <= 4 ? wordKey[1] : wordKey[2]);
        },
        translate: function (number, withoutSuffix, key) {
            var wordKey = translator.words[key];
            if (key.length === 1) {
                return withoutSuffix ? wordKey[0] : wordKey[1];
            } else {
                return number + ' ' + translator.correctGrammaticalCase(number, wordKey);
            }
        }
    };

    hooks.defineLocale('me', {
        months: 'januar_februar_mart_april_maj_jun_jul_avgust_septembar_oktobar_novembar_decembar'.split('_'),
        monthsShort: 'jan._feb._mar._apr._maj_jun_jul_avg._sep._okt._nov._dec.'.split('_'),
        monthsParseExact : true,
        weekdays: 'nedjelja_ponedjeljak_utorak_srijeda_četvrtak_petak_subota'.split('_'),
        weekdaysShort: 'ned._pon._uto._sri._čet._pet._sub.'.split('_'),
        weekdaysMin: 'ne_po_ut_sr_če_pe_su'.split('_'),
        weekdaysParseExact : true,
        longDateFormat: {
            LT: 'H:mm',
            LTS : 'H:mm:ss',
            L: 'DD.MM.YYYY',
            LL: 'D. MMMM YYYY',
            LLL: 'D. MMMM YYYY H:mm',
            LLLL: 'dddd, D. MMMM YYYY H:mm'
        },
        calendar: {
            sameDay: '[danas u] LT',
            nextDay: '[sjutra u] LT',

            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[u] [nedjelju] [u] LT';
                    case 3:
                        return '[u] [srijedu] [u] LT';
                    case 6:
                        return '[u] [subotu] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[u] dddd [u] LT';
                }
            },
            lastDay  : '[juče u] LT',
            lastWeek : function () {
                var lastWeekDays = [
                    '[prošle] [nedjelje] [u] LT',
                    '[prošlog] [ponedjeljka] [u] LT',
                    '[prošlog] [utorka] [u] LT',
                    '[prošle] [srijede] [u] LT',
                    '[prošlog] [četvrtka] [u] LT',
                    '[prošlog] [petka] [u] LT',
                    '[prošle] [subote] [u] LT'
                ];
                return lastWeekDays[this.day()];
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'za %s',
            past   : 'prije %s',
            s      : 'nekoliko sekundi',
            ss     : translator.translate,
            m      : translator.translate,
            mm     : translator.translate,
            h      : translator.translate,
            hh     : translator.translate,
            d      : 'dan',
            dd     : translator.translate,
            M      : 'mjesec',
            MM     : translator.translate,
            y      : 'godinu',
            yy     : translator.translate
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('mi', {
        months: 'Kohi-tāte_Hui-tanguru_Poutū-te-rangi_Paenga-whāwhā_Haratua_Pipiri_Hōngoingoi_Here-turi-kōkā_Mahuru_Whiringa-ā-nuku_Whiringa-ā-rangi_Hakihea'.split('_'),
        monthsShort: 'Kohi_Hui_Pou_Pae_Hara_Pipi_Hōngoi_Here_Mahu_Whi-nu_Whi-ra_Haki'.split('_'),
        monthsRegex: /(?:['a-z\u0101\u014D\u016B]+\-?){1,3}/i,
        monthsStrictRegex: /(?:['a-z\u0101\u014D\u016B]+\-?){1,3}/i,
        monthsShortRegex: /(?:['a-z\u0101\u014D\u016B]+\-?){1,3}/i,
        monthsShortStrictRegex: /(?:['a-z\u0101\u014D\u016B]+\-?){1,2}/i,
        weekdays: 'Rātapu_Mane_Tūrei_Wenerei_Tāite_Paraire_Hātarei'.split('_'),
        weekdaysShort: 'Ta_Ma_Tū_We_Tāi_Pa_Hā'.split('_'),
        weekdaysMin: 'Ta_Ma_Tū_We_Tāi_Pa_Hā'.split('_'),
        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY [i] HH:mm',
            LLLL: 'dddd, D MMMM YYYY [i] HH:mm'
        },
        calendar: {
            sameDay: '[i teie mahana, i] LT',
            nextDay: '[apopo i] LT',
            nextWeek: 'dddd [i] LT',
            lastDay: '[inanahi i] LT',
            lastWeek: 'dddd [whakamutunga i] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: 'i roto i %s',
            past: '%s i mua',
            s: 'te hēkona ruarua',
            ss: '%d hēkona',
            m: 'he meneti',
            mm: '%d meneti',
            h: 'te haora',
            hh: '%d haora',
            d: 'he ra',
            dd: '%d ra',
            M: 'he marama',
            MM: '%d marama',
            y: 'he tau',
            yy: '%d tau'
        },
        dayOfMonthOrdinalParse: /\d{1,2}º/,
        ordinal: '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('mk', {
        months : 'јануари_февруари_март_април_мај_јуни_јули_август_септември_октомври_ноември_декември'.split('_'),
        monthsShort : 'јан_фев_мар_апр_мај_јун_јул_авг_сеп_окт_ное_дек'.split('_'),
        weekdays : 'недела_понеделник_вторник_среда_четврток_петок_сабота'.split('_'),
        weekdaysShort : 'нед_пон_вто_сре_чет_пет_саб'.split('_'),
        weekdaysMin : 'нe_пo_вт_ср_че_пе_сa'.split('_'),
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'D.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY H:mm',
            LLLL : 'dddd, D MMMM YYYY H:mm'
        },
        calendar : {
            sameDay : '[Денес во] LT',
            nextDay : '[Утре во] LT',
            nextWeek : '[Во] dddd [во] LT',
            lastDay : '[Вчера во] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                    case 6:
                        return '[Изминатата] dddd [во] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[Изминатиот] dddd [во] LT';
                }
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'после %s',
            past : 'пред %s',
            s : 'неколку секунди',
            ss : '%d секунди',
            m : 'минута',
            mm : '%d минути',
            h : 'час',
            hh : '%d часа',
            d : 'ден',
            dd : '%d дена',
            M : 'месец',
            MM : '%d месеци',
            y : 'година',
            yy : '%d години'
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(ев|ен|ти|ви|ри|ми)/,
        ordinal : function (number) {
            var lastDigit = number % 10,
                last2Digits = number % 100;
            if (number === 0) {
                return number + '-ев';
            } else if (last2Digits === 0) {
                return number + '-ен';
            } else if (last2Digits > 10 && last2Digits < 20) {
                return number + '-ти';
            } else if (lastDigit === 1) {
                return number + '-ви';
            } else if (lastDigit === 2) {
                return number + '-ри';
            } else if (lastDigit === 7 || lastDigit === 8) {
                return number + '-ми';
            } else {
                return number + '-ти';
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ml', {
        months : 'ജനുവരി_ഫെബ്രുവരി_മാർച്ച്_ഏപ്രിൽ_മേയ്_ജൂൺ_ജൂലൈ_ഓഗസ്റ്റ്_സെപ്റ്റംബർ_ഒക്ടോബർ_നവംബർ_ഡിസംബർ'.split('_'),
        monthsShort : 'ജനു._ഫെബ്രു._മാർ._ഏപ്രി._മേയ്_ജൂൺ_ജൂലൈ._ഓഗ._സെപ്റ്റ._ഒക്ടോ._നവം._ഡിസം.'.split('_'),
        monthsParseExact : true,
        weekdays : 'ഞായറാഴ്ച_തിങ്കളാഴ്ച_ചൊവ്വാഴ്ച_ബുധനാഴ്ച_വ്യാഴാഴ്ച_വെള്ളിയാഴ്ച_ശനിയാഴ്ച'.split('_'),
        weekdaysShort : 'ഞായർ_തിങ്കൾ_ചൊവ്വ_ബുധൻ_വ്യാഴം_വെള്ളി_ശനി'.split('_'),
        weekdaysMin : 'ഞാ_തി_ചൊ_ബു_വ്യാ_വെ_ശ'.split('_'),
        longDateFormat : {
            LT : 'A h:mm -നു',
            LTS : 'A h:mm:ss -നു',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm -നു',
            LLLL : 'dddd, D MMMM YYYY, A h:mm -നു'
        },
        calendar : {
            sameDay : '[ഇന്ന്] LT',
            nextDay : '[നാളെ] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[ഇന്നലെ] LT',
            lastWeek : '[കഴിഞ്ഞ] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s കഴിഞ്ഞ്',
            past : '%s മുൻപ്',
            s : 'അൽപ നിമിഷങ്ങൾ',
            ss : '%d സെക്കൻഡ്',
            m : 'ഒരു മിനിറ്റ്',
            mm : '%d മിനിറ്റ്',
            h : 'ഒരു മണിക്കൂർ',
            hh : '%d മണിക്കൂർ',
            d : 'ഒരു ദിവസം',
            dd : '%d ദിവസം',
            M : 'ഒരു മാസം',
            MM : '%d മാസം',
            y : 'ഒരു വർഷം',
            yy : '%d വർഷം'
        },
        meridiemParse: /രാത്രി|രാവിലെ|ഉച്ച കഴിഞ്ഞ്|വൈകുന്നേരം|രാത്രി/i,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if ((meridiem === 'രാത്രി' && hour >= 4) ||
                    meridiem === 'ഉച്ച കഴിഞ്ഞ്' ||
                    meridiem === 'വൈകുന്നേരം') {
                return hour + 12;
            } else {
                return hour;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'രാത്രി';
            } else if (hour < 12) {
                return 'രാവിലെ';
            } else if (hour < 17) {
                return 'ഉച്ച കഴിഞ്ഞ്';
            } else if (hour < 20) {
                return 'വൈകുന്നേരം';
            } else {
                return 'രാത്രി';
            }
        }
    });

    //! moment.js locale configuration

    function translate$7(number, withoutSuffix, key, isFuture) {
        switch (key) {
            case 's':
                return withoutSuffix ? 'хэдхэн секунд' : 'хэдхэн секундын';
            case 'ss':
                return number + (withoutSuffix ? ' секунд' : ' секундын');
            case 'm':
            case 'mm':
                return number + (withoutSuffix ? ' минут' : ' минутын');
            case 'h':
            case 'hh':
                return number + (withoutSuffix ? ' цаг' : ' цагийн');
            case 'd':
            case 'dd':
                return number + (withoutSuffix ? ' өдөр' : ' өдрийн');
            case 'M':
            case 'MM':
                return number + (withoutSuffix ? ' сар' : ' сарын');
            case 'y':
            case 'yy':
                return number + (withoutSuffix ? ' жил' : ' жилийн');
            default:
                return number;
        }
    }

    hooks.defineLocale('mn', {
        months : 'Нэгдүгээр сар_Хоёрдугаар сар_Гуравдугаар сар_Дөрөвдүгээр сар_Тавдугаар сар_Зургадугаар сар_Долдугаар сар_Наймдугаар сар_Есдүгээр сар_Аравдугаар сар_Арван нэгдүгээр сар_Арван хоёрдугаар сар'.split('_'),
        monthsShort : '1 сар_2 сар_3 сар_4 сар_5 сар_6 сар_7 сар_8 сар_9 сар_10 сар_11 сар_12 сар'.split('_'),
        monthsParseExact : true,
        weekdays : 'Ням_Даваа_Мягмар_Лхагва_Пүрэв_Баасан_Бямба'.split('_'),
        weekdaysShort : 'Ням_Дав_Мяг_Лха_Пүр_Баа_Бям'.split('_'),
        weekdaysMin : 'Ня_Да_Мя_Лх_Пү_Ба_Бя'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'YYYY оны MMMMын D',
            LLL : 'YYYY оны MMMMын D HH:mm',
            LLLL : 'dddd, YYYY оны MMMMын D HH:mm'
        },
        meridiemParse: /ҮӨ|ҮХ/i,
        isPM : function (input) {
            return input === 'ҮХ';
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ҮӨ';
            } else {
                return 'ҮХ';
            }
        },
        calendar : {
            sameDay : '[Өнөөдөр] LT',
            nextDay : '[Маргааш] LT',
            nextWeek : '[Ирэх] dddd LT',
            lastDay : '[Өчигдөр] LT',
            lastWeek : '[Өнгөрсөн] dddd LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s дараа',
            past : '%s өмнө',
            s : translate$7,
            ss : translate$7,
            m : translate$7,
            mm : translate$7,
            h : translate$7,
            hh : translate$7,
            d : translate$7,
            dd : translate$7,
            M : translate$7,
            MM : translate$7,
            y : translate$7,
            yy : translate$7
        },
        dayOfMonthOrdinalParse: /\d{1,2} өдөр/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'DDD':
                    return number + ' өдөр';
                default:
                    return number;
            }
        }
    });

    //! moment.js locale configuration

    var symbolMap$b = {
        '1': '१',
        '2': '२',
        '3': '३',
        '4': '४',
        '5': '५',
        '6': '६',
        '7': '७',
        '8': '८',
        '9': '९',
        '0': '०'
    },
    numberMap$a = {
        '१': '1',
        '२': '2',
        '३': '3',
        '४': '4',
        '५': '5',
        '६': '6',
        '७': '7',
        '८': '8',
        '९': '9',
        '०': '0'
    };

    function relativeTimeMr(number, withoutSuffix, string, isFuture)
    {
        var output = '';
        if (withoutSuffix) {
            switch (string) {
                case 's': output = 'काही सेकंद'; break;
                case 'ss': output = '%d सेकंद'; break;
                case 'm': output = 'एक मिनिट'; break;
                case 'mm': output = '%d मिनिटे'; break;
                case 'h': output = 'एक तास'; break;
                case 'hh': output = '%d तास'; break;
                case 'd': output = 'एक दिवस'; break;
                case 'dd': output = '%d दिवस'; break;
                case 'M': output = 'एक महिना'; break;
                case 'MM': output = '%d महिने'; break;
                case 'y': output = 'एक वर्ष'; break;
                case 'yy': output = '%d वर्षे'; break;
            }
        }
        else {
            switch (string) {
                case 's': output = 'काही सेकंदां'; break;
                case 'ss': output = '%d सेकंदां'; break;
                case 'm': output = 'एका मिनिटा'; break;
                case 'mm': output = '%d मिनिटां'; break;
                case 'h': output = 'एका तासा'; break;
                case 'hh': output = '%d तासां'; break;
                case 'd': output = 'एका दिवसा'; break;
                case 'dd': output = '%d दिवसां'; break;
                case 'M': output = 'एका महिन्या'; break;
                case 'MM': output = '%d महिन्यां'; break;
                case 'y': output = 'एका वर्षा'; break;
                case 'yy': output = '%d वर्षां'; break;
            }
        }
        return output.replace(/%d/i, number);
    }

    hooks.defineLocale('mr', {
        months : 'जानेवारी_फेब्रुवारी_मार्च_एप्रिल_मे_जून_जुलै_ऑगस्ट_सप्टेंबर_ऑक्टोबर_नोव्हेंबर_डिसेंबर'.split('_'),
        monthsShort: 'जाने._फेब्रु._मार्च._एप्रि._मे._जून._जुलै._ऑग._सप्टें._ऑक्टो._नोव्हें._डिसें.'.split('_'),
        monthsParseExact : true,
        weekdays : 'रविवार_सोमवार_मंगळवार_बुधवार_गुरूवार_शुक्रवार_शनिवार'.split('_'),
        weekdaysShort : 'रवि_सोम_मंगळ_बुध_गुरू_शुक्र_शनि'.split('_'),
        weekdaysMin : 'र_सो_मं_बु_गु_शु_श'.split('_'),
        longDateFormat : {
            LT : 'A h:mm वाजता',
            LTS : 'A h:mm:ss वाजता',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm वाजता',
            LLLL : 'dddd, D MMMM YYYY, A h:mm वाजता'
        },
        calendar : {
            sameDay : '[आज] LT',
            nextDay : '[उद्या] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[काल] LT',
            lastWeek: '[मागील] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future: '%sमध्ये',
            past: '%sपूर्वी',
            s: relativeTimeMr,
            ss: relativeTimeMr,
            m: relativeTimeMr,
            mm: relativeTimeMr,
            h: relativeTimeMr,
            hh: relativeTimeMr,
            d: relativeTimeMr,
            dd: relativeTimeMr,
            M: relativeTimeMr,
            MM: relativeTimeMr,
            y: relativeTimeMr,
            yy: relativeTimeMr
        },
        preparse: function (string) {
            return string.replace(/[१२३४५६७८९०]/g, function (match) {
                return numberMap$a[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$b[match];
            });
        },
        meridiemParse: /रात्री|सकाळी|दुपारी|सायंकाळी/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'रात्री') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'सकाळी') {
                return hour;
            } else if (meridiem === 'दुपारी') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'सायंकाळी') {
                return hour + 12;
            }
        },
        meridiem: function (hour, minute, isLower) {
            if (hour < 4) {
                return 'रात्री';
            } else if (hour < 10) {
                return 'सकाळी';
            } else if (hour < 17) {
                return 'दुपारी';
            } else if (hour < 20) {
                return 'सायंकाळी';
            } else {
                return 'रात्री';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ms-my', {
        months : 'Januari_Februari_Mac_April_Mei_Jun_Julai_Ogos_September_Oktober_November_Disember'.split('_'),
        monthsShort : 'Jan_Feb_Mac_Apr_Mei_Jun_Jul_Ogs_Sep_Okt_Nov_Dis'.split('_'),
        weekdays : 'Ahad_Isnin_Selasa_Rabu_Khamis_Jumaat_Sabtu'.split('_'),
        weekdaysShort : 'Ahd_Isn_Sel_Rab_Kha_Jum_Sab'.split('_'),
        weekdaysMin : 'Ah_Is_Sl_Rb_Km_Jm_Sb'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY [pukul] HH.mm',
            LLLL : 'dddd, D MMMM YYYY [pukul] HH.mm'
        },
        meridiemParse: /pagi|tengahari|petang|malam/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'pagi') {
                return hour;
            } else if (meridiem === 'tengahari') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'petang' || meridiem === 'malam') {
                return hour + 12;
            }
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 11) {
                return 'pagi';
            } else if (hours < 15) {
                return 'tengahari';
            } else if (hours < 19) {
                return 'petang';
            } else {
                return 'malam';
            }
        },
        calendar : {
            sameDay : '[Hari ini pukul] LT',
            nextDay : '[Esok pukul] LT',
            nextWeek : 'dddd [pukul] LT',
            lastDay : '[Kelmarin pukul] LT',
            lastWeek : 'dddd [lepas pukul] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dalam %s',
            past : '%s yang lepas',
            s : 'beberapa saat',
            ss : '%d saat',
            m : 'seminit',
            mm : '%d minit',
            h : 'sejam',
            hh : '%d jam',
            d : 'sehari',
            dd : '%d hari',
            M : 'sebulan',
            MM : '%d bulan',
            y : 'setahun',
            yy : '%d tahun'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ms', {
        months : 'Januari_Februari_Mac_April_Mei_Jun_Julai_Ogos_September_Oktober_November_Disember'.split('_'),
        monthsShort : 'Jan_Feb_Mac_Apr_Mei_Jun_Jul_Ogs_Sep_Okt_Nov_Dis'.split('_'),
        weekdays : 'Ahad_Isnin_Selasa_Rabu_Khamis_Jumaat_Sabtu'.split('_'),
        weekdaysShort : 'Ahd_Isn_Sel_Rab_Kha_Jum_Sab'.split('_'),
        weekdaysMin : 'Ah_Is_Sl_Rb_Km_Jm_Sb'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY [pukul] HH.mm',
            LLLL : 'dddd, D MMMM YYYY [pukul] HH.mm'
        },
        meridiemParse: /pagi|tengahari|petang|malam/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'pagi') {
                return hour;
            } else if (meridiem === 'tengahari') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'petang' || meridiem === 'malam') {
                return hour + 12;
            }
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 11) {
                return 'pagi';
            } else if (hours < 15) {
                return 'tengahari';
            } else if (hours < 19) {
                return 'petang';
            } else {
                return 'malam';
            }
        },
        calendar : {
            sameDay : '[Hari ini pukul] LT',
            nextDay : '[Esok pukul] LT',
            nextWeek : 'dddd [pukul] LT',
            lastDay : '[Kelmarin pukul] LT',
            lastWeek : 'dddd [lepas pukul] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'dalam %s',
            past : '%s yang lepas',
            s : 'beberapa saat',
            ss : '%d saat',
            m : 'seminit',
            mm : '%d minit',
            h : 'sejam',
            hh : '%d jam',
            d : 'sehari',
            dd : '%d hari',
            M : 'sebulan',
            MM : '%d bulan',
            y : 'setahun',
            yy : '%d tahun'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('mt', {
        months : 'Jannar_Frar_Marzu_April_Mejju_Ġunju_Lulju_Awwissu_Settembru_Ottubru_Novembru_Diċembru'.split('_'),
        monthsShort : 'Jan_Fra_Mar_Apr_Mej_Ġun_Lul_Aww_Set_Ott_Nov_Diċ'.split('_'),
        weekdays : 'Il-Ħadd_It-Tnejn_It-Tlieta_L-Erbgħa_Il-Ħamis_Il-Ġimgħa_Is-Sibt'.split('_'),
        weekdaysShort : 'Ħad_Tne_Tli_Erb_Ħam_Ġim_Sib'.split('_'),
        weekdaysMin : 'Ħa_Tn_Tl_Er_Ħa_Ġi_Si'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Illum fil-]LT',
            nextDay : '[Għada fil-]LT',
            nextWeek : 'dddd [fil-]LT',
            lastDay : '[Il-bieraħ fil-]LT',
            lastWeek : 'dddd [li għadda] [fil-]LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'f’ %s',
            past : '%s ilu',
            s : 'ftit sekondi',
            ss : '%d sekondi',
            m : 'minuta',
            mm : '%d minuti',
            h : 'siegħa',
            hh : '%d siegħat',
            d : 'ġurnata',
            dd : '%d ġranet',
            M : 'xahar',
            MM : '%d xhur',
            y : 'sena',
            yy : '%d sni'
        },
        dayOfMonthOrdinalParse : /\d{1,2}º/,
        ordinal: '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$c = {
        '1': '၁',
        '2': '၂',
        '3': '၃',
        '4': '၄',
        '5': '၅',
        '6': '၆',
        '7': '၇',
        '8': '၈',
        '9': '၉',
        '0': '၀'
    }, numberMap$b = {
        '၁': '1',
        '၂': '2',
        '၃': '3',
        '၄': '4',
        '၅': '5',
        '၆': '6',
        '၇': '7',
        '၈': '8',
        '၉': '9',
        '၀': '0'
    };

    hooks.defineLocale('my', {
        months: 'ဇန်နဝါရီ_ဖေဖော်ဝါရီ_မတ်_ဧပြီ_မေ_ဇွန်_ဇူလိုင်_သြဂုတ်_စက်တင်ဘာ_အောက်တိုဘာ_နိုဝင်ဘာ_ဒီဇင်ဘာ'.split('_'),
        monthsShort: 'ဇန်_ဖေ_မတ်_ပြီ_မေ_ဇွန်_လိုင်_သြ_စက်_အောက်_နို_ဒီ'.split('_'),
        weekdays: 'တနင်္ဂနွေ_တနင်္လာ_အင်္ဂါ_ဗုဒ္ဓဟူး_ကြာသပတေး_သောကြာ_စနေ'.split('_'),
        weekdaysShort: 'နွေ_လာ_ဂါ_ဟူး_ကြာ_သော_နေ'.split('_'),
        weekdaysMin: 'နွေ_လာ_ဂါ_ဟူး_ကြာ_သော_နေ'.split('_'),

        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'DD/MM/YYYY',
            LL: 'D MMMM YYYY',
            LLL: 'D MMMM YYYY HH:mm',
            LLLL: 'dddd D MMMM YYYY HH:mm'
        },
        calendar: {
            sameDay: '[ယနေ.] LT [မှာ]',
            nextDay: '[မနက်ဖြန်] LT [မှာ]',
            nextWeek: 'dddd LT [မှာ]',
            lastDay: '[မနေ.က] LT [မှာ]',
            lastWeek: '[ပြီးခဲ့သော] dddd LT [မှာ]',
            sameElse: 'L'
        },
        relativeTime: {
            future: 'လာမည့် %s မှာ',
            past: 'လွန်ခဲ့သော %s က',
            s: 'စက္ကန်.အနည်းငယ်',
            ss : '%d စက္ကန့်',
            m: 'တစ်မိနစ်',
            mm: '%d မိနစ်',
            h: 'တစ်နာရီ',
            hh: '%d နာရီ',
            d: 'တစ်ရက်',
            dd: '%d ရက်',
            M: 'တစ်လ',
            MM: '%d လ',
            y: 'တစ်နှစ်',
            yy: '%d နှစ်'
        },
        preparse: function (string) {
            return string.replace(/[၁၂၃၄၅၆၇၈၉၀]/g, function (match) {
                return numberMap$b[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$c[match];
            });
        },
        week: {
            dow: 1, // Monday is the first day of the week.
            doy: 4 // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('nb', {
        months : 'januar_februar_mars_april_mai_juni_juli_august_september_oktober_november_desember'.split('_'),
        monthsShort : 'jan._feb._mars_april_mai_juni_juli_aug._sep._okt._nov._des.'.split('_'),
        monthsParseExact : true,
        weekdays : 'søndag_mandag_tirsdag_onsdag_torsdag_fredag_lørdag'.split('_'),
        weekdaysShort : 'sø._ma._ti._on._to._fr._lø.'.split('_'),
        weekdaysMin : 'sø_ma_ti_on_to_fr_lø'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY [kl.] HH:mm',
            LLLL : 'dddd D. MMMM YYYY [kl.] HH:mm'
        },
        calendar : {
            sameDay: '[i dag kl.] LT',
            nextDay: '[i morgen kl.] LT',
            nextWeek: 'dddd [kl.] LT',
            lastDay: '[i går kl.] LT',
            lastWeek: '[forrige] dddd [kl.] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'om %s',
            past : '%s siden',
            s : 'noen sekunder',
            ss : '%d sekunder',
            m : 'ett minutt',
            mm : '%d minutter',
            h : 'en time',
            hh : '%d timer',
            d : 'en dag',
            dd : '%d dager',
            M : 'en måned',
            MM : '%d måneder',
            y : 'ett år',
            yy : '%d år'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$d = {
        '1': '१',
        '2': '२',
        '3': '३',
        '4': '४',
        '5': '५',
        '6': '६',
        '7': '७',
        '8': '८',
        '9': '९',
        '0': '०'
    },
    numberMap$c = {
        '१': '1',
        '२': '2',
        '३': '3',
        '४': '4',
        '५': '5',
        '६': '6',
        '७': '7',
        '८': '8',
        '९': '9',
        '०': '0'
    };

    hooks.defineLocale('ne', {
        months : 'जनवरी_फेब्रुवरी_मार्च_अप्रिल_मई_जुन_जुलाई_अगष्ट_सेप्टेम्बर_अक्टोबर_नोभेम्बर_डिसेम्बर'.split('_'),
        monthsShort : 'जन._फेब्रु._मार्च_अप्रि._मई_जुन_जुलाई._अग._सेप्ट._अक्टो._नोभे._डिसे.'.split('_'),
        monthsParseExact : true,
        weekdays : 'आइतबार_सोमबार_मङ्गलबार_बुधबार_बिहिबार_शुक्रबार_शनिबार'.split('_'),
        weekdaysShort : 'आइत._सोम._मङ्गल._बुध._बिहि._शुक्र._शनि.'.split('_'),
        weekdaysMin : 'आ._सो._मं._बु._बि._शु._श.'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'Aको h:mm बजे',
            LTS : 'Aको h:mm:ss बजे',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, Aको h:mm बजे',
            LLLL : 'dddd, D MMMM YYYY, Aको h:mm बजे'
        },
        preparse: function (string) {
            return string.replace(/[१२३४५६७८९०]/g, function (match) {
                return numberMap$c[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$d[match];
            });
        },
        meridiemParse: /राति|बिहान|दिउँसो|साँझ/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'राति') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'बिहान') {
                return hour;
            } else if (meridiem === 'दिउँसो') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'साँझ') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 3) {
                return 'राति';
            } else if (hour < 12) {
                return 'बिहान';
            } else if (hour < 16) {
                return 'दिउँसो';
            } else if (hour < 20) {
                return 'साँझ';
            } else {
                return 'राति';
            }
        },
        calendar : {
            sameDay : '[आज] LT',
            nextDay : '[भोलि] LT',
            nextWeek : '[आउँदो] dddd[,] LT',
            lastDay : '[हिजो] LT',
            lastWeek : '[गएको] dddd[,] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%sमा',
            past : '%s अगाडि',
            s : 'केही क्षण',
            ss : '%d सेकेण्ड',
            m : 'एक मिनेट',
            mm : '%d मिनेट',
            h : 'एक घण्टा',
            hh : '%d घण्टा',
            d : 'एक दिन',
            dd : '%d दिन',
            M : 'एक महिना',
            MM : '%d महिना',
            y : 'एक बर्ष',
            yy : '%d बर्ष'
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortWithDots$1 = 'jan._feb._mrt._apr._mei_jun._jul._aug._sep._okt._nov._dec.'.split('_'),
        monthsShortWithoutDots$1 = 'jan_feb_mrt_apr_mei_jun_jul_aug_sep_okt_nov_dec'.split('_');

    var monthsParse$4 = [/^jan/i, /^feb/i, /^maart|mrt.?$/i, /^apr/i, /^mei$/i, /^jun[i.]?$/i, /^jul[i.]?$/i, /^aug/i, /^sep/i, /^okt/i, /^nov/i, /^dec/i];
    var monthsRegex$5 = /^(januari|februari|maart|april|mei|ju[nl]i|augustus|september|oktober|november|december|jan\.?|feb\.?|mrt\.?|apr\.?|ju[nl]\.?|aug\.?|sep\.?|okt\.?|nov\.?|dec\.?)/i;

    hooks.defineLocale('nl-be', {
        months : 'januari_februari_maart_april_mei_juni_juli_augustus_september_oktober_november_december'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortWithDots$1;
            } else if (/-MMM-/.test(format)) {
                return monthsShortWithoutDots$1[m.month()];
            } else {
                return monthsShortWithDots$1[m.month()];
            }
        },

        monthsRegex: monthsRegex$5,
        monthsShortRegex: monthsRegex$5,
        monthsStrictRegex: /^(januari|februari|maart|april|mei|ju[nl]i|augustus|september|oktober|november|december)/i,
        monthsShortStrictRegex: /^(jan\.?|feb\.?|mrt\.?|apr\.?|mei|ju[nl]\.?|aug\.?|sep\.?|okt\.?|nov\.?|dec\.?)/i,

        monthsParse : monthsParse$4,
        longMonthsParse : monthsParse$4,
        shortMonthsParse : monthsParse$4,

        weekdays : 'zondag_maandag_dinsdag_woensdag_donderdag_vrijdag_zaterdag'.split('_'),
        weekdaysShort : 'zo._ma._di._wo._do._vr._za.'.split('_'),
        weekdaysMin : 'zo_ma_di_wo_do_vr_za'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[vandaag om] LT',
            nextDay: '[morgen om] LT',
            nextWeek: 'dddd [om] LT',
            lastDay: '[gisteren om] LT',
            lastWeek: '[afgelopen] dddd [om] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'over %s',
            past : '%s geleden',
            s : 'een paar seconden',
            ss : '%d seconden',
            m : 'één minuut',
            mm : '%d minuten',
            h : 'één uur',
            hh : '%d uur',
            d : 'één dag',
            dd : '%d dagen',
            M : 'één maand',
            MM : '%d maanden',
            y : 'één jaar',
            yy : '%d jaar'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(ste|de)/,
        ordinal : function (number) {
            return number + ((number === 1 || number === 8 || number >= 20) ? 'ste' : 'de');
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsShortWithDots$2 = 'jan._feb._mrt._apr._mei_jun._jul._aug._sep._okt._nov._dec.'.split('_'),
        monthsShortWithoutDots$2 = 'jan_feb_mrt_apr_mei_jun_jul_aug_sep_okt_nov_dec'.split('_');

    var monthsParse$5 = [/^jan/i, /^feb/i, /^maart|mrt.?$/i, /^apr/i, /^mei$/i, /^jun[i.]?$/i, /^jul[i.]?$/i, /^aug/i, /^sep/i, /^okt/i, /^nov/i, /^dec/i];
    var monthsRegex$6 = /^(januari|februari|maart|april|mei|ju[nl]i|augustus|september|oktober|november|december|jan\.?|feb\.?|mrt\.?|apr\.?|ju[nl]\.?|aug\.?|sep\.?|okt\.?|nov\.?|dec\.?)/i;

    hooks.defineLocale('nl', {
        months : 'januari_februari_maart_april_mei_juni_juli_augustus_september_oktober_november_december'.split('_'),
        monthsShort : function (m, format) {
            if (!m) {
                return monthsShortWithDots$2;
            } else if (/-MMM-/.test(format)) {
                return monthsShortWithoutDots$2[m.month()];
            } else {
                return monthsShortWithDots$2[m.month()];
            }
        },

        monthsRegex: monthsRegex$6,
        monthsShortRegex: monthsRegex$6,
        monthsStrictRegex: /^(januari|februari|maart|april|mei|ju[nl]i|augustus|september|oktober|november|december)/i,
        monthsShortStrictRegex: /^(jan\.?|feb\.?|mrt\.?|apr\.?|mei|ju[nl]\.?|aug\.?|sep\.?|okt\.?|nov\.?|dec\.?)/i,

        monthsParse : monthsParse$5,
        longMonthsParse : monthsParse$5,
        shortMonthsParse : monthsParse$5,

        weekdays : 'zondag_maandag_dinsdag_woensdag_donderdag_vrijdag_zaterdag'.split('_'),
        weekdaysShort : 'zo._ma._di._wo._do._vr._za.'.split('_'),
        weekdaysMin : 'zo_ma_di_wo_do_vr_za'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD-MM-YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[vandaag om] LT',
            nextDay: '[morgen om] LT',
            nextWeek: 'dddd [om] LT',
            lastDay: '[gisteren om] LT',
            lastWeek: '[afgelopen] dddd [om] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'over %s',
            past : '%s geleden',
            s : 'een paar seconden',
            ss : '%d seconden',
            m : 'één minuut',
            mm : '%d minuten',
            h : 'één uur',
            hh : '%d uur',
            d : 'één dag',
            dd : '%d dagen',
            M : 'één maand',
            MM : '%d maanden',
            y : 'één jaar',
            yy : '%d jaar'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(ste|de)/,
        ordinal : function (number) {
            return number + ((number === 1 || number === 8 || number >= 20) ? 'ste' : 'de');
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('nn', {
        months : 'januar_februar_mars_april_mai_juni_juli_august_september_oktober_november_desember'.split('_'),
        monthsShort : 'jan_feb_mar_apr_mai_jun_jul_aug_sep_okt_nov_des'.split('_'),
        weekdays : 'sundag_måndag_tysdag_onsdag_torsdag_fredag_laurdag'.split('_'),
        weekdaysShort : 'sun_mån_tys_ons_tor_fre_lau'.split('_'),
        weekdaysMin : 'su_må_ty_on_to_fr_lø'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY [kl.] H:mm',
            LLLL : 'dddd D. MMMM YYYY [kl.] HH:mm'
        },
        calendar : {
            sameDay: '[I dag klokka] LT',
            nextDay: '[I morgon klokka] LT',
            nextWeek: 'dddd [klokka] LT',
            lastDay: '[I går klokka] LT',
            lastWeek: '[Føregåande] dddd [klokka] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'om %s',
            past : '%s sidan',
            s : 'nokre sekund',
            ss : '%d sekund',
            m : 'eit minutt',
            mm : '%d minutt',
            h : 'ein time',
            hh : '%d timar',
            d : 'ein dag',
            dd : '%d dagar',
            M : 'ein månad',
            MM : '%d månader',
            y : 'eit år',
            yy : '%d år'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$e = {
        '1': '੧',
        '2': '੨',
        '3': '੩',
        '4': '੪',
        '5': '੫',
        '6': '੬',
        '7': '੭',
        '8': '੮',
        '9': '੯',
        '0': '੦'
    },
    numberMap$d = {
        '੧': '1',
        '੨': '2',
        '੩': '3',
        '੪': '4',
        '੫': '5',
        '੬': '6',
        '੭': '7',
        '੮': '8',
        '੯': '9',
        '੦': '0'
    };

    hooks.defineLocale('pa-in', {
        // There are months name as per Nanakshahi Calendar but they are not used as rigidly in modern Punjabi.
        months : 'ਜਨਵਰੀ_ਫ਼ਰਵਰੀ_ਮਾਰਚ_ਅਪ੍ਰੈਲ_ਮਈ_ਜੂਨ_ਜੁਲਾਈ_ਅਗਸਤ_ਸਤੰਬਰ_ਅਕਤੂਬਰ_ਨਵੰਬਰ_ਦਸੰਬਰ'.split('_'),
        monthsShort : 'ਜਨਵਰੀ_ਫ਼ਰਵਰੀ_ਮਾਰਚ_ਅਪ੍ਰੈਲ_ਮਈ_ਜੂਨ_ਜੁਲਾਈ_ਅਗਸਤ_ਸਤੰਬਰ_ਅਕਤੂਬਰ_ਨਵੰਬਰ_ਦਸੰਬਰ'.split('_'),
        weekdays : 'ਐਤਵਾਰ_ਸੋਮਵਾਰ_ਮੰਗਲਵਾਰ_ਬੁਧਵਾਰ_ਵੀਰਵਾਰ_ਸ਼ੁੱਕਰਵਾਰ_ਸ਼ਨੀਚਰਵਾਰ'.split('_'),
        weekdaysShort : 'ਐਤ_ਸੋਮ_ਮੰਗਲ_ਬੁਧ_ਵੀਰ_ਸ਼ੁਕਰ_ਸ਼ਨੀ'.split('_'),
        weekdaysMin : 'ਐਤ_ਸੋਮ_ਮੰਗਲ_ਬੁਧ_ਵੀਰ_ਸ਼ੁਕਰ_ਸ਼ਨੀ'.split('_'),
        longDateFormat : {
            LT : 'A h:mm ਵਜੇ',
            LTS : 'A h:mm:ss ਵਜੇ',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm ਵਜੇ',
            LLLL : 'dddd, D MMMM YYYY, A h:mm ਵਜੇ'
        },
        calendar : {
            sameDay : '[ਅਜ] LT',
            nextDay : '[ਕਲ] LT',
            nextWeek : '[ਅਗਲਾ] dddd, LT',
            lastDay : '[ਕਲ] LT',
            lastWeek : '[ਪਿਛਲੇ] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s ਵਿੱਚ',
            past : '%s ਪਿਛਲੇ',
            s : 'ਕੁਝ ਸਕਿੰਟ',
            ss : '%d ਸਕਿੰਟ',
            m : 'ਇਕ ਮਿੰਟ',
            mm : '%d ਮਿੰਟ',
            h : 'ਇੱਕ ਘੰਟਾ',
            hh : '%d ਘੰਟੇ',
            d : 'ਇੱਕ ਦਿਨ',
            dd : '%d ਦਿਨ',
            M : 'ਇੱਕ ਮਹੀਨਾ',
            MM : '%d ਮਹੀਨੇ',
            y : 'ਇੱਕ ਸਾਲ',
            yy : '%d ਸਾਲ'
        },
        preparse: function (string) {
            return string.replace(/[੧੨੩੪੫੬੭੮੯੦]/g, function (match) {
                return numberMap$d[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$e[match];
            });
        },
        // Punjabi notation for meridiems are quite fuzzy in practice. While there exists
        // a rigid notion of a 'Pahar' it is not used as rigidly in modern Punjabi.
        meridiemParse: /ਰਾਤ|ਸਵੇਰ|ਦੁਪਹਿਰ|ਸ਼ਾਮ/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'ਰਾਤ') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'ਸਵੇਰ') {
                return hour;
            } else if (meridiem === 'ਦੁਪਹਿਰ') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'ਸ਼ਾਮ') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'ਰਾਤ';
            } else if (hour < 10) {
                return 'ਸਵੇਰ';
            } else if (hour < 17) {
                return 'ਦੁਪਹਿਰ';
            } else if (hour < 20) {
                return 'ਸ਼ਾਮ';
            } else {
                return 'ਰਾਤ';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var monthsNominative = 'styczeń_luty_marzec_kwiecień_maj_czerwiec_lipiec_sierpień_wrzesień_październik_listopad_grudzień'.split('_'),
        monthsSubjective = 'stycznia_lutego_marca_kwietnia_maja_czerwca_lipca_sierpnia_września_października_listopada_grudnia'.split('_');
    function plural$3(n) {
        return (n % 10 < 5) && (n % 10 > 1) && ((~~(n / 10) % 10) !== 1);
    }
    function translate$8(number, withoutSuffix, key) {
        var result = number + ' ';
        switch (key) {
            case 'ss':
                return result + (plural$3(number) ? 'sekundy' : 'sekund');
            case 'm':
                return withoutSuffix ? 'minuta' : 'minutę';
            case 'mm':
                return result + (plural$3(number) ? 'minuty' : 'minut');
            case 'h':
                return withoutSuffix  ? 'godzina'  : 'godzinę';
            case 'hh':
                return result + (plural$3(number) ? 'godziny' : 'godzin');
            case 'MM':
                return result + (plural$3(number) ? 'miesiące' : 'miesięcy');
            case 'yy':
                return result + (plural$3(number) ? 'lata' : 'lat');
        }
    }

    hooks.defineLocale('pl', {
        months : function (momentToFormat, format) {
            if (!momentToFormat) {
                return monthsNominative;
            } else if (format === '') {
                // Hack: if format empty we know this is used to generate
                // RegExp by moment. Give then back both valid forms of months
                // in RegExp ready format.
                return '(' + monthsSubjective[momentToFormat.month()] + '|' + monthsNominative[momentToFormat.month()] + ')';
            } else if (/D MMMM/.test(format)) {
                return monthsSubjective[momentToFormat.month()];
            } else {
                return monthsNominative[momentToFormat.month()];
            }
        },
        monthsShort : 'sty_lut_mar_kwi_maj_cze_lip_sie_wrz_paź_lis_gru'.split('_'),
        weekdays : 'niedziela_poniedziałek_wtorek_środa_czwartek_piątek_sobota'.split('_'),
        weekdaysShort : 'ndz_pon_wt_śr_czw_pt_sob'.split('_'),
        weekdaysMin : 'Nd_Pn_Wt_Śr_Cz_Pt_So'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Dziś o] LT',
            nextDay: '[Jutro o] LT',
            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[W niedzielę o] LT';

                    case 2:
                        return '[We wtorek o] LT';

                    case 3:
                        return '[W środę o] LT';

                    case 6:
                        return '[W sobotę o] LT';

                    default:
                        return '[W] dddd [o] LT';
                }
            },
            lastDay: '[Wczoraj o] LT',
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[W zeszłą niedzielę o] LT';
                    case 3:
                        return '[W zeszłą środę o] LT';
                    case 6:
                        return '[W zeszłą sobotę o] LT';
                    default:
                        return '[W zeszły] dddd [o] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'za %s',
            past : '%s temu',
            s : 'kilka sekund',
            ss : translate$8,
            m : translate$8,
            mm : translate$8,
            h : translate$8,
            hh : translate$8,
            d : '1 dzień',
            dd : '%d dni',
            M : 'miesiąc',
            MM : translate$8,
            y : 'rok',
            yy : translate$8
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('pt-br', {
        months : 'Janeiro_Fevereiro_Março_Abril_Maio_Junho_Julho_Agosto_Setembro_Outubro_Novembro_Dezembro'.split('_'),
        monthsShort : 'Jan_Fev_Mar_Abr_Mai_Jun_Jul_Ago_Set_Out_Nov_Dez'.split('_'),
        weekdays : 'Domingo_Segunda-feira_Terça-feira_Quarta-feira_Quinta-feira_Sexta-feira_Sábado'.split('_'),
        weekdaysShort : 'Dom_Seg_Ter_Qua_Qui_Sex_Sáb'.split('_'),
        weekdaysMin : 'Do_2ª_3ª_4ª_5ª_6ª_Sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY [às] HH:mm',
            LLLL : 'dddd, D [de] MMMM [de] YYYY [às] HH:mm'
        },
        calendar : {
            sameDay: '[Hoje às] LT',
            nextDay: '[Amanhã às] LT',
            nextWeek: 'dddd [às] LT',
            lastDay: '[Ontem às] LT',
            lastWeek: function () {
                return (this.day() === 0 || this.day() === 6) ?
                    '[Último] dddd [às] LT' : // Saturday + Sunday
                    '[Última] dddd [às] LT'; // Monday - Friday
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'em %s',
            past : 'há %s',
            s : 'poucos segundos',
            ss : '%d segundos',
            m : 'um minuto',
            mm : '%d minutos',
            h : 'uma hora',
            hh : '%d horas',
            d : 'um dia',
            dd : '%d dias',
            M : 'um mês',
            MM : '%d meses',
            y : 'um ano',
            yy : '%d anos'
        },
        dayOfMonthOrdinalParse: /\d{1,2}º/,
        ordinal : '%dº'
    });

    //! moment.js locale configuration

    hooks.defineLocale('pt', {
        months : 'Janeiro_Fevereiro_Março_Abril_Maio_Junho_Julho_Agosto_Setembro_Outubro_Novembro_Dezembro'.split('_'),
        monthsShort : 'Jan_Fev_Mar_Abr_Mai_Jun_Jul_Ago_Set_Out_Nov_Dez'.split('_'),
        weekdays : 'Domingo_Segunda-feira_Terça-feira_Quarta-feira_Quinta-feira_Sexta-feira_Sábado'.split('_'),
        weekdaysShort : 'Dom_Seg_Ter_Qua_Qui_Sex_Sáb'.split('_'),
        weekdaysMin : 'Do_2ª_3ª_4ª_5ª_6ª_Sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D [de] MMMM [de] YYYY',
            LLL : 'D [de] MMMM [de] YYYY HH:mm',
            LLLL : 'dddd, D [de] MMMM [de] YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Hoje às] LT',
            nextDay: '[Amanhã às] LT',
            nextWeek: 'dddd [às] LT',
            lastDay: '[Ontem às] LT',
            lastWeek: function () {
                return (this.day() === 0 || this.day() === 6) ?
                    '[Último] dddd [às] LT' : // Saturday + Sunday
                    '[Última] dddd [às] LT'; // Monday - Friday
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'em %s',
            past : 'há %s',
            s : 'segundos',
            ss : '%d segundos',
            m : 'um minuto',
            mm : '%d minutos',
            h : 'uma hora',
            hh : '%d horas',
            d : 'um dia',
            dd : '%d dias',
            M : 'um mês',
            MM : '%d meses',
            y : 'um ano',
            yy : '%d anos'
        },
        dayOfMonthOrdinalParse: /\d{1,2}º/,
        ordinal : '%dº',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function relativeTimeWithPlural$2(number, withoutSuffix, key) {
        var format = {
                'ss': 'secunde',
                'mm': 'minute',
                'hh': 'ore',
                'dd': 'zile',
                'MM': 'luni',
                'yy': 'ani'
            },
            separator = ' ';
        if (number % 100 >= 20 || (number >= 100 && number % 100 === 0)) {
            separator = ' de ';
        }
        return number + separator + format[key];
    }

    hooks.defineLocale('ro', {
        months : 'ianuarie_februarie_martie_aprilie_mai_iunie_iulie_august_septembrie_octombrie_noiembrie_decembrie'.split('_'),
        monthsShort : 'ian._febr._mart._apr._mai_iun._iul._aug._sept._oct._nov._dec.'.split('_'),
        monthsParseExact: true,
        weekdays : 'duminică_luni_marți_miercuri_joi_vineri_sâmbătă'.split('_'),
        weekdaysShort : 'Dum_Lun_Mar_Mie_Joi_Vin_Sâm'.split('_'),
        weekdaysMin : 'Du_Lu_Ma_Mi_Jo_Vi_Sâ'.split('_'),
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY H:mm',
            LLLL : 'dddd, D MMMM YYYY H:mm'
        },
        calendar : {
            sameDay: '[azi la] LT',
            nextDay: '[mâine la] LT',
            nextWeek: 'dddd [la] LT',
            lastDay: '[ieri la] LT',
            lastWeek: '[fosta] dddd [la] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'peste %s',
            past : '%s în urmă',
            s : 'câteva secunde',
            ss : relativeTimeWithPlural$2,
            m : 'un minut',
            mm : relativeTimeWithPlural$2,
            h : 'o oră',
            hh : relativeTimeWithPlural$2,
            d : 'o zi',
            dd : relativeTimeWithPlural$2,
            M : 'o lună',
            MM : relativeTimeWithPlural$2,
            y : 'un an',
            yy : relativeTimeWithPlural$2
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function plural$4(word, num) {
        var forms = word.split('_');
        return num % 10 === 1 && num % 100 !== 11 ? forms[0] : (num % 10 >= 2 && num % 10 <= 4 && (num % 100 < 10 || num % 100 >= 20) ? forms[1] : forms[2]);
    }
    function relativeTimeWithPlural$3(number, withoutSuffix, key) {
        var format = {
            'ss': withoutSuffix ? 'секунда_секунды_секунд' : 'секунду_секунды_секунд',
            'mm': withoutSuffix ? 'минута_минуты_минут' : 'минуту_минуты_минут',
            'hh': 'час_часа_часов',
            'dd': 'день_дня_дней',
            'MM': 'месяц_месяца_месяцев',
            'yy': 'год_года_лет'
        };
        if (key === 'm') {
            return withoutSuffix ? 'минута' : 'минуту';
        }
        else {
            return number + ' ' + plural$4(format[key], +number);
        }
    }
    var monthsParse$6 = [/^янв/i, /^фев/i, /^мар/i, /^апр/i, /^ма[йя]/i, /^июн/i, /^июл/i, /^авг/i, /^сен/i, /^окт/i, /^ноя/i, /^дек/i];

    // http://new.gramota.ru/spravka/rules/139-prop : § 103
    // Сокращения месяцев: http://new.gramota.ru/spravka/buro/search-answer?s=242637
    // CLDR data:          http://www.unicode.org/cldr/charts/28/summary/ru.html#1753
    hooks.defineLocale('ru', {
        months : {
            format: 'января_февраля_марта_апреля_мая_июня_июля_августа_сентября_октября_ноября_декабря'.split('_'),
            standalone: 'январь_февраль_март_апрель_май_июнь_июль_август_сентябрь_октябрь_ноябрь_декабрь'.split('_')
        },
        monthsShort : {
            // по CLDR именно "июл." и "июн.", но какой смысл менять букву на точку ?
            format: 'янв._февр._мар._апр._мая_июня_июля_авг._сент._окт._нояб._дек.'.split('_'),
            standalone: 'янв._февр._март_апр._май_июнь_июль_авг._сент._окт._нояб._дек.'.split('_')
        },
        weekdays : {
            standalone: 'воскресенье_понедельник_вторник_среда_четверг_пятница_суббота'.split('_'),
            format: 'воскресенье_понедельник_вторник_среду_четверг_пятницу_субботу'.split('_'),
            isFormat: /\[ ?[Вв] ?(?:прошлую|следующую|эту)? ?\] ?dddd/
        },
        weekdaysShort : 'вс_пн_вт_ср_чт_пт_сб'.split('_'),
        weekdaysMin : 'вс_пн_вт_ср_чт_пт_сб'.split('_'),
        monthsParse : monthsParse$6,
        longMonthsParse : monthsParse$6,
        shortMonthsParse : monthsParse$6,

        // полные названия с падежами, по три буквы, для некоторых, по 4 буквы, сокращения с точкой и без точки
        monthsRegex: /^(январ[ья]|янв\.?|феврал[ья]|февр?\.?|марта?|мар\.?|апрел[ья]|апр\.?|ма[йя]|июн[ья]|июн\.?|июл[ья]|июл\.?|августа?|авг\.?|сентябр[ья]|сент?\.?|октябр[ья]|окт\.?|ноябр[ья]|нояб?\.?|декабр[ья]|дек\.?)/i,

        // копия предыдущего
        monthsShortRegex: /^(январ[ья]|янв\.?|феврал[ья]|февр?\.?|марта?|мар\.?|апрел[ья]|апр\.?|ма[йя]|июн[ья]|июн\.?|июл[ья]|июл\.?|августа?|авг\.?|сентябр[ья]|сент?\.?|октябр[ья]|окт\.?|ноябр[ья]|нояб?\.?|декабр[ья]|дек\.?)/i,

        // полные названия с падежами
        monthsStrictRegex: /^(январ[яь]|феврал[яь]|марта?|апрел[яь]|ма[яй]|июн[яь]|июл[яь]|августа?|сентябр[яь]|октябр[яь]|ноябр[яь]|декабр[яь])/i,

        // Выражение, которое соотвествует только сокращённым формам
        monthsShortStrictRegex: /^(янв\.|февр?\.|мар[т.]|апр\.|ма[яй]|июн[ья.]|июл[ья.]|авг\.|сент?\.|окт\.|нояб?\.|дек\.)/i,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY г.',
            LLL : 'D MMMM YYYY г., H:mm',
            LLLL : 'dddd, D MMMM YYYY г., H:mm'
        },
        calendar : {
            sameDay: '[Сегодня, в] LT',
            nextDay: '[Завтра, в] LT',
            lastDay: '[Вчера, в] LT',
            nextWeek: function (now) {
                if (now.week() !== this.week()) {
                    switch (this.day()) {
                        case 0:
                            return '[В следующее] dddd, [в] LT';
                        case 1:
                        case 2:
                        case 4:
                            return '[В следующий] dddd, [в] LT';
                        case 3:
                        case 5:
                        case 6:
                            return '[В следующую] dddd, [в] LT';
                    }
                } else {
                    if (this.day() === 2) {
                        return '[Во] dddd, [в] LT';
                    } else {
                        return '[В] dddd, [в] LT';
                    }
                }
            },
            lastWeek: function (now) {
                if (now.week() !== this.week()) {
                    switch (this.day()) {
                        case 0:
                            return '[В прошлое] dddd, [в] LT';
                        case 1:
                        case 2:
                        case 4:
                            return '[В прошлый] dddd, [в] LT';
                        case 3:
                        case 5:
                        case 6:
                            return '[В прошлую] dddd, [в] LT';
                    }
                } else {
                    if (this.day() === 2) {
                        return '[Во] dddd, [в] LT';
                    } else {
                        return '[В] dddd, [в] LT';
                    }
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'через %s',
            past : '%s назад',
            s : 'несколько секунд',
            ss : relativeTimeWithPlural$3,
            m : relativeTimeWithPlural$3,
            mm : relativeTimeWithPlural$3,
            h : 'час',
            hh : relativeTimeWithPlural$3,
            d : 'день',
            dd : relativeTimeWithPlural$3,
            M : 'месяц',
            MM : relativeTimeWithPlural$3,
            y : 'год',
            yy : relativeTimeWithPlural$3
        },
        meridiemParse: /ночи|утра|дня|вечера/i,
        isPM : function (input) {
            return /^(дня|вечера)$/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'ночи';
            } else if (hour < 12) {
                return 'утра';
            } else if (hour < 17) {
                return 'дня';
            } else {
                return 'вечера';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(й|го|я)/,
        ordinal: function (number, period) {
            switch (period) {
                case 'M':
                case 'd':
                case 'DDD':
                    return number + '-й';
                case 'D':
                    return number + '-го';
                case 'w':
                case 'W':
                    return number + '-я';
                default:
                    return number;
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var months$8 = [
        'جنوري',
        'فيبروري',
        'مارچ',
        'اپريل',
        'مئي',
        'جون',
        'جولاءِ',
        'آگسٽ',
        'سيپٽمبر',
        'آڪٽوبر',
        'نومبر',
        'ڊسمبر'
    ];
    var days$1 = [
        'آچر',
        'سومر',
        'اڱارو',
        'اربع',
        'خميس',
        'جمع',
        'ڇنڇر'
    ];

    hooks.defineLocale('sd', {
        months : months$8,
        monthsShort : months$8,
        weekdays : days$1,
        weekdaysShort : days$1,
        weekdaysMin : days$1,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd، D MMMM YYYY HH:mm'
        },
        meridiemParse: /صبح|شام/,
        isPM : function (input) {
            return 'شام' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'صبح';
            }
            return 'شام';
        },
        calendar : {
            sameDay : '[اڄ] LT',
            nextDay : '[سڀاڻي] LT',
            nextWeek : 'dddd [اڳين هفتي تي] LT',
            lastDay : '[ڪالهه] LT',
            lastWeek : '[گزريل هفتي] dddd [تي] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s پوء',
            past : '%s اڳ',
            s : 'چند سيڪنڊ',
            ss : '%d سيڪنڊ',
            m : 'هڪ منٽ',
            mm : '%d منٽ',
            h : 'هڪ ڪلاڪ',
            hh : '%d ڪلاڪ',
            d : 'هڪ ڏينهن',
            dd : '%d ڏينهن',
            M : 'هڪ مهينو',
            MM : '%d مهينا',
            y : 'هڪ سال',
            yy : '%d سال'
        },
        preparse: function (string) {
            return string.replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/,/g, '،');
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('se', {
        months : 'ođđajagemánnu_guovvamánnu_njukčamánnu_cuoŋománnu_miessemánnu_geassemánnu_suoidnemánnu_borgemánnu_čakčamánnu_golggotmánnu_skábmamánnu_juovlamánnu'.split('_'),
        monthsShort : 'ođđj_guov_njuk_cuo_mies_geas_suoi_borg_čakč_golg_skáb_juov'.split('_'),
        weekdays : 'sotnabeaivi_vuossárga_maŋŋebárga_gaskavahkku_duorastat_bearjadat_lávvardat'.split('_'),
        weekdaysShort : 'sotn_vuos_maŋ_gask_duor_bear_láv'.split('_'),
        weekdaysMin : 's_v_m_g_d_b_L'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'MMMM D. [b.] YYYY',
            LLL : 'MMMM D. [b.] YYYY [ti.] HH:mm',
            LLLL : 'dddd, MMMM D. [b.] YYYY [ti.] HH:mm'
        },
        calendar : {
            sameDay: '[otne ti] LT',
            nextDay: '[ihttin ti] LT',
            nextWeek: 'dddd [ti] LT',
            lastDay: '[ikte ti] LT',
            lastWeek: '[ovddit] dddd [ti] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : '%s geažes',
            past : 'maŋit %s',
            s : 'moadde sekunddat',
            ss: '%d sekunddat',
            m : 'okta minuhta',
            mm : '%d minuhtat',
            h : 'okta diimmu',
            hh : '%d diimmut',
            d : 'okta beaivi',
            dd : '%d beaivvit',
            M : 'okta mánnu',
            MM : '%d mánut',
            y : 'okta jahki',
            yy : '%d jagit'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    /*jshint -W100*/
    hooks.defineLocale('si', {
        months : 'ජනවාරි_පෙබරවාරි_මාර්තු_අප්‍රේල්_මැයි_ජූනි_ජූලි_අගෝස්තු_සැප්තැම්බර්_ඔක්තෝබර්_නොවැම්බර්_දෙසැම්බර්'.split('_'),
        monthsShort : 'ජන_පෙබ_මාර්_අප්_මැයි_ජූනි_ජූලි_අගෝ_සැප්_ඔක්_නොවැ_දෙසැ'.split('_'),
        weekdays : 'ඉරිදා_සඳුදා_අඟහරුවාදා_බදාදා_බ්‍රහස්පතින්දා_සිකුරාදා_සෙනසුරාදා'.split('_'),
        weekdaysShort : 'ඉරි_සඳු_අඟ_බදා_බ්‍රහ_සිකු_සෙන'.split('_'),
        weekdaysMin : 'ඉ_ස_අ_බ_බ්‍ර_සි_සෙ'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'a h:mm',
            LTS : 'a h:mm:ss',
            L : 'YYYY/MM/DD',
            LL : 'YYYY MMMM D',
            LLL : 'YYYY MMMM D, a h:mm',
            LLLL : 'YYYY MMMM D [වැනි] dddd, a h:mm:ss'
        },
        calendar : {
            sameDay : '[අද] LT[ට]',
            nextDay : '[හෙට] LT[ට]',
            nextWeek : 'dddd LT[ට]',
            lastDay : '[ඊයේ] LT[ට]',
            lastWeek : '[පසුගිය] dddd LT[ට]',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%sකින්',
            past : '%sකට පෙර',
            s : 'තත්පර කිහිපය',
            ss : 'තත්පර %d',
            m : 'මිනිත්තුව',
            mm : 'මිනිත්තු %d',
            h : 'පැය',
            hh : 'පැය %d',
            d : 'දිනය',
            dd : 'දින %d',
            M : 'මාසය',
            MM : 'මාස %d',
            y : 'වසර',
            yy : 'වසර %d'
        },
        dayOfMonthOrdinalParse: /\d{1,2} වැනි/,
        ordinal : function (number) {
            return number + ' වැනි';
        },
        meridiemParse : /පෙර වරු|පස් වරු|පෙ.ව|ප.ව./,
        isPM : function (input) {
            return input === 'ප.ව.' || input === 'පස් වරු';
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours > 11) {
                return isLower ? 'ප.ව.' : 'පස් වරු';
            } else {
                return isLower ? 'පෙ.ව.' : 'පෙර වරු';
            }
        }
    });

    //! moment.js locale configuration

    var months$9 = 'január_február_marec_apríl_máj_jún_júl_august_september_október_november_december'.split('_'),
        monthsShort$6 = 'jan_feb_mar_apr_máj_jún_júl_aug_sep_okt_nov_dec'.split('_');
    function plural$5(n) {
        return (n > 1) && (n < 5);
    }
    function translate$9(number, withoutSuffix, key, isFuture) {
        var result = number + ' ';
        switch (key) {
            case 's':  // a few seconds / in a few seconds / a few seconds ago
                return (withoutSuffix || isFuture) ? 'pár sekúnd' : 'pár sekundami';
            case 'ss': // 9 seconds / in 9 seconds / 9 seconds ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'sekundy' : 'sekúnd');
                } else {
                    return result + 'sekundami';
                }
                break;
            case 'm':  // a minute / in a minute / a minute ago
                return withoutSuffix ? 'minúta' : (isFuture ? 'minútu' : 'minútou');
            case 'mm': // 9 minutes / in 9 minutes / 9 minutes ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'minúty' : 'minút');
                } else {
                    return result + 'minútami';
                }
                break;
            case 'h':  // an hour / in an hour / an hour ago
                return withoutSuffix ? 'hodina' : (isFuture ? 'hodinu' : 'hodinou');
            case 'hh': // 9 hours / in 9 hours / 9 hours ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'hodiny' : 'hodín');
                } else {
                    return result + 'hodinami';
                }
                break;
            case 'd':  // a day / in a day / a day ago
                return (withoutSuffix || isFuture) ? 'deň' : 'dňom';
            case 'dd': // 9 days / in 9 days / 9 days ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'dni' : 'dní');
                } else {
                    return result + 'dňami';
                }
                break;
            case 'M':  // a month / in a month / a month ago
                return (withoutSuffix || isFuture) ? 'mesiac' : 'mesiacom';
            case 'MM': // 9 months / in 9 months / 9 months ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'mesiace' : 'mesiacov');
                } else {
                    return result + 'mesiacmi';
                }
                break;
            case 'y':  // a year / in a year / a year ago
                return (withoutSuffix || isFuture) ? 'rok' : 'rokom';
            case 'yy': // 9 years / in 9 years / 9 years ago
                if (withoutSuffix || isFuture) {
                    return result + (plural$5(number) ? 'roky' : 'rokov');
                } else {
                    return result + 'rokmi';
                }
                break;
        }
    }

    hooks.defineLocale('sk', {
        months : months$9,
        monthsShort : monthsShort$6,
        weekdays : 'nedeľa_pondelok_utorok_streda_štvrtok_piatok_sobota'.split('_'),
        weekdaysShort : 'ne_po_ut_st_št_pi_so'.split('_'),
        weekdaysMin : 'ne_po_ut_st_št_pi_so'.split('_'),
        longDateFormat : {
            LT: 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd D. MMMM YYYY H:mm'
        },
        calendar : {
            sameDay: '[dnes o] LT',
            nextDay: '[zajtra o] LT',
            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[v nedeľu o] LT';
                    case 1:
                    case 2:
                        return '[v] dddd [o] LT';
                    case 3:
                        return '[v stredu o] LT';
                    case 4:
                        return '[vo štvrtok o] LT';
                    case 5:
                        return '[v piatok o] LT';
                    case 6:
                        return '[v sobotu o] LT';
                }
            },
            lastDay: '[včera o] LT',
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[minulú nedeľu o] LT';
                    case 1:
                    case 2:
                        return '[minulý] dddd [o] LT';
                    case 3:
                        return '[minulú stredu o] LT';
                    case 4:
                    case 5:
                        return '[minulý] dddd [o] LT';
                    case 6:
                        return '[minulú sobotu o] LT';
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'za %s',
            past : 'pred %s',
            s : translate$9,
            ss : translate$9,
            m : translate$9,
            mm : translate$9,
            h : translate$9,
            hh : translate$9,
            d : translate$9,
            dd : translate$9,
            M : translate$9,
            MM : translate$9,
            y : translate$9,
            yy : translate$9
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function processRelativeTime$6(number, withoutSuffix, key, isFuture) {
        var result = number + ' ';
        switch (key) {
            case 's':
                return withoutSuffix || isFuture ? 'nekaj sekund' : 'nekaj sekundami';
            case 'ss':
                if (number === 1) {
                    result += withoutSuffix ? 'sekundo' : 'sekundi';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'sekundi' : 'sekundah';
                } else if (number < 5) {
                    result += withoutSuffix || isFuture ? 'sekunde' : 'sekundah';
                } else {
                    result += 'sekund';
                }
                return result;
            case 'm':
                return withoutSuffix ? 'ena minuta' : 'eno minuto';
            case 'mm':
                if (number === 1) {
                    result += withoutSuffix ? 'minuta' : 'minuto';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'minuti' : 'minutama';
                } else if (number < 5) {
                    result += withoutSuffix || isFuture ? 'minute' : 'minutami';
                } else {
                    result += withoutSuffix || isFuture ? 'minut' : 'minutami';
                }
                return result;
            case 'h':
                return withoutSuffix ? 'ena ura' : 'eno uro';
            case 'hh':
                if (number === 1) {
                    result += withoutSuffix ? 'ura' : 'uro';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'uri' : 'urama';
                } else if (number < 5) {
                    result += withoutSuffix || isFuture ? 'ure' : 'urami';
                } else {
                    result += withoutSuffix || isFuture ? 'ur' : 'urami';
                }
                return result;
            case 'd':
                return withoutSuffix || isFuture ? 'en dan' : 'enim dnem';
            case 'dd':
                if (number === 1) {
                    result += withoutSuffix || isFuture ? 'dan' : 'dnem';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'dni' : 'dnevoma';
                } else {
                    result += withoutSuffix || isFuture ? 'dni' : 'dnevi';
                }
                return result;
            case 'M':
                return withoutSuffix || isFuture ? 'en mesec' : 'enim mesecem';
            case 'MM':
                if (number === 1) {
                    result += withoutSuffix || isFuture ? 'mesec' : 'mesecem';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'meseca' : 'mesecema';
                } else if (number < 5) {
                    result += withoutSuffix || isFuture ? 'mesece' : 'meseci';
                } else {
                    result += withoutSuffix || isFuture ? 'mesecev' : 'meseci';
                }
                return result;
            case 'y':
                return withoutSuffix || isFuture ? 'eno leto' : 'enim letom';
            case 'yy':
                if (number === 1) {
                    result += withoutSuffix || isFuture ? 'leto' : 'letom';
                } else if (number === 2) {
                    result += withoutSuffix || isFuture ? 'leti' : 'letoma';
                } else if (number < 5) {
                    result += withoutSuffix || isFuture ? 'leta' : 'leti';
                } else {
                    result += withoutSuffix || isFuture ? 'let' : 'leti';
                }
                return result;
        }
    }

    hooks.defineLocale('sl', {
        months : 'januar_februar_marec_april_maj_junij_julij_avgust_september_oktober_november_december'.split('_'),
        monthsShort : 'jan._feb._mar._apr._maj._jun._jul._avg._sep._okt._nov._dec.'.split('_'),
        monthsParseExact: true,
        weekdays : 'nedelja_ponedeljek_torek_sreda_četrtek_petek_sobota'.split('_'),
        weekdaysShort : 'ned._pon._tor._sre._čet._pet._sob.'.split('_'),
        weekdaysMin : 'ne_po_to_sr_če_pe_so'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM YYYY',
            LLL : 'D. MMMM YYYY H:mm',
            LLLL : 'dddd, D. MMMM YYYY H:mm'
        },
        calendar : {
            sameDay  : '[danes ob] LT',
            nextDay  : '[jutri ob] LT',

            nextWeek : function () {
                switch (this.day()) {
                    case 0:
                        return '[v] [nedeljo] [ob] LT';
                    case 3:
                        return '[v] [sredo] [ob] LT';
                    case 6:
                        return '[v] [soboto] [ob] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[v] dddd [ob] LT';
                }
            },
            lastDay  : '[včeraj ob] LT',
            lastWeek : function () {
                switch (this.day()) {
                    case 0:
                        return '[prejšnjo] [nedeljo] [ob] LT';
                    case 3:
                        return '[prejšnjo] [sredo] [ob] LT';
                    case 6:
                        return '[prejšnjo] [soboto] [ob] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[prejšnji] dddd [ob] LT';
                }
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'čez %s',
            past   : 'pred %s',
            s      : processRelativeTime$6,
            ss     : processRelativeTime$6,
            m      : processRelativeTime$6,
            mm     : processRelativeTime$6,
            h      : processRelativeTime$6,
            hh     : processRelativeTime$6,
            d      : processRelativeTime$6,
            dd     : processRelativeTime$6,
            M      : processRelativeTime$6,
            MM     : processRelativeTime$6,
            y      : processRelativeTime$6,
            yy     : processRelativeTime$6
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('sq', {
        months : 'Janar_Shkurt_Mars_Prill_Maj_Qershor_Korrik_Gusht_Shtator_Tetor_Nëntor_Dhjetor'.split('_'),
        monthsShort : 'Jan_Shk_Mar_Pri_Maj_Qer_Kor_Gus_Sht_Tet_Nën_Dhj'.split('_'),
        weekdays : 'E Diel_E Hënë_E Martë_E Mërkurë_E Enjte_E Premte_E Shtunë'.split('_'),
        weekdaysShort : 'Die_Hën_Mar_Mër_Enj_Pre_Sht'.split('_'),
        weekdaysMin : 'D_H_Ma_Më_E_P_Sh'.split('_'),
        weekdaysParseExact : true,
        meridiemParse: /PD|MD/,
        isPM: function (input) {
            return input.charAt(0) === 'M';
        },
        meridiem : function (hours, minutes, isLower) {
            return hours < 12 ? 'PD' : 'MD';
        },
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Sot në] LT',
            nextDay : '[Nesër në] LT',
            nextWeek : 'dddd [në] LT',
            lastDay : '[Dje në] LT',
            lastWeek : 'dddd [e kaluar në] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'në %s',
            past : '%s më parë',
            s : 'disa sekonda',
            ss : '%d sekonda',
            m : 'një minutë',
            mm : '%d minuta',
            h : 'një orë',
            hh : '%d orë',
            d : 'një ditë',
            dd : '%d ditë',
            M : 'një muaj',
            MM : '%d muaj',
            y : 'një vit',
            yy : '%d vite'
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var translator$1 = {
        words: { //Different grammatical cases
            ss: ['секунда', 'секунде', 'секунди'],
            m: ['један минут', 'једне минуте'],
            mm: ['минут', 'минуте', 'минута'],
            h: ['један сат', 'једног сата'],
            hh: ['сат', 'сата', 'сати'],
            dd: ['дан', 'дана', 'дана'],
            MM: ['месец', 'месеца', 'месеци'],
            yy: ['година', 'године', 'година']
        },
        correctGrammaticalCase: function (number, wordKey) {
            return number === 1 ? wordKey[0] : (number >= 2 && number <= 4 ? wordKey[1] : wordKey[2]);
        },
        translate: function (number, withoutSuffix, key) {
            var wordKey = translator$1.words[key];
            if (key.length === 1) {
                return withoutSuffix ? wordKey[0] : wordKey[1];
            } else {
                return number + ' ' + translator$1.correctGrammaticalCase(number, wordKey);
            }
        }
    };

    hooks.defineLocale('sr-cyrl', {
        months: 'јануар_фебруар_март_април_мај_јун_јул_август_септембар_октобар_новембар_децембар'.split('_'),
        monthsShort: 'јан._феб._мар._апр._мај_јун_јул_авг._сеп._окт._нов._дец.'.split('_'),
        monthsParseExact: true,
        weekdays: 'недеља_понедељак_уторак_среда_четвртак_петак_субота'.split('_'),
        weekdaysShort: 'нед._пон._уто._сре._чет._пет._суб.'.split('_'),
        weekdaysMin: 'не_по_ут_ср_че_пе_су'.split('_'),
        weekdaysParseExact : true,
        longDateFormat: {
            LT: 'H:mm',
            LTS : 'H:mm:ss',
            L: 'DD.MM.YYYY',
            LL: 'D. MMMM YYYY',
            LLL: 'D. MMMM YYYY H:mm',
            LLLL: 'dddd, D. MMMM YYYY H:mm'
        },
        calendar: {
            sameDay: '[данас у] LT',
            nextDay: '[сутра у] LT',
            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[у] [недељу] [у] LT';
                    case 3:
                        return '[у] [среду] [у] LT';
                    case 6:
                        return '[у] [суботу] [у] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[у] dddd [у] LT';
                }
            },
            lastDay  : '[јуче у] LT',
            lastWeek : function () {
                var lastWeekDays = [
                    '[прошле] [недеље] [у] LT',
                    '[прошлог] [понедељка] [у] LT',
                    '[прошлог] [уторка] [у] LT',
                    '[прошле] [среде] [у] LT',
                    '[прошлог] [четвртка] [у] LT',
                    '[прошлог] [петка] [у] LT',
                    '[прошле] [суботе] [у] LT'
                ];
                return lastWeekDays[this.day()];
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'за %s',
            past   : 'пре %s',
            s      : 'неколико секунди',
            ss     : translator$1.translate,
            m      : translator$1.translate,
            mm     : translator$1.translate,
            h      : translator$1.translate,
            hh     : translator$1.translate,
            d      : 'дан',
            dd     : translator$1.translate,
            M      : 'месец',
            MM     : translator$1.translate,
            y      : 'годину',
            yy     : translator$1.translate
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var translator$2 = {
        words: { //Different grammatical cases
            ss: ['sekunda', 'sekunde', 'sekundi'],
            m: ['jedan minut', 'jedne minute'],
            mm: ['minut', 'minute', 'minuta'],
            h: ['jedan sat', 'jednog sata'],
            hh: ['sat', 'sata', 'sati'],
            dd: ['dan', 'dana', 'dana'],
            MM: ['mesec', 'meseca', 'meseci'],
            yy: ['godina', 'godine', 'godina']
        },
        correctGrammaticalCase: function (number, wordKey) {
            return number === 1 ? wordKey[0] : (number >= 2 && number <= 4 ? wordKey[1] : wordKey[2]);
        },
        translate: function (number, withoutSuffix, key) {
            var wordKey = translator$2.words[key];
            if (key.length === 1) {
                return withoutSuffix ? wordKey[0] : wordKey[1];
            } else {
                return number + ' ' + translator$2.correctGrammaticalCase(number, wordKey);
            }
        }
    };

    hooks.defineLocale('sr', {
        months: 'januar_februar_mart_april_maj_jun_jul_avgust_septembar_oktobar_novembar_decembar'.split('_'),
        monthsShort: 'jan._feb._mar._apr._maj_jun_jul_avg._sep._okt._nov._dec.'.split('_'),
        monthsParseExact: true,
        weekdays: 'nedelja_ponedeljak_utorak_sreda_četvrtak_petak_subota'.split('_'),
        weekdaysShort: 'ned._pon._uto._sre._čet._pet._sub.'.split('_'),
        weekdaysMin: 'ne_po_ut_sr_če_pe_su'.split('_'),
        weekdaysParseExact : true,
        longDateFormat: {
            LT: 'H:mm',
            LTS : 'H:mm:ss',
            L: 'DD.MM.YYYY',
            LL: 'D. MMMM YYYY',
            LLL: 'D. MMMM YYYY H:mm',
            LLLL: 'dddd, D. MMMM YYYY H:mm'
        },
        calendar: {
            sameDay: '[danas u] LT',
            nextDay: '[sutra u] LT',
            nextWeek: function () {
                switch (this.day()) {
                    case 0:
                        return '[u] [nedelju] [u] LT';
                    case 3:
                        return '[u] [sredu] [u] LT';
                    case 6:
                        return '[u] [subotu] [u] LT';
                    case 1:
                    case 2:
                    case 4:
                    case 5:
                        return '[u] dddd [u] LT';
                }
            },
            lastDay  : '[juče u] LT',
            lastWeek : function () {
                var lastWeekDays = [
                    '[prošle] [nedelje] [u] LT',
                    '[prošlog] [ponedeljka] [u] LT',
                    '[prošlog] [utorka] [u] LT',
                    '[prošle] [srede] [u] LT',
                    '[prošlog] [četvrtka] [u] LT',
                    '[prošlog] [petka] [u] LT',
                    '[prošle] [subote] [u] LT'
                ];
                return lastWeekDays[this.day()];
            },
            sameElse : 'L'
        },
        relativeTime : {
            future : 'za %s',
            past   : 'pre %s',
            s      : 'nekoliko sekundi',
            ss     : translator$2.translate,
            m      : translator$2.translate,
            mm     : translator$2.translate,
            h      : translator$2.translate,
            hh     : translator$2.translate,
            d      : 'dan',
            dd     : translator$2.translate,
            M      : 'mesec',
            MM     : translator$2.translate,
            y      : 'godinu',
            yy     : translator$2.translate
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('ss', {
        months : "Bhimbidvwane_Indlovana_Indlov'lenkhulu_Mabasa_Inkhwekhweti_Inhlaba_Kholwane_Ingci_Inyoni_Imphala_Lweti_Ingongoni".split('_'),
        monthsShort : 'Bhi_Ina_Inu_Mab_Ink_Inh_Kho_Igc_Iny_Imp_Lwe_Igo'.split('_'),
        weekdays : 'Lisontfo_Umsombuluko_Lesibili_Lesitsatfu_Lesine_Lesihlanu_Umgcibelo'.split('_'),
        weekdaysShort : 'Lis_Umb_Lsb_Les_Lsi_Lsh_Umg'.split('_'),
        weekdaysMin : 'Li_Us_Lb_Lt_Ls_Lh_Ug'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendar : {
            sameDay : '[Namuhla nga] LT',
            nextDay : '[Kusasa nga] LT',
            nextWeek : 'dddd [nga] LT',
            lastDay : '[Itolo nga] LT',
            lastWeek : 'dddd [leliphelile] [nga] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'nga %s',
            past : 'wenteka nga %s',
            s : 'emizuzwana lomcane',
            ss : '%d mzuzwana',
            m : 'umzuzu',
            mm : '%d emizuzu',
            h : 'lihora',
            hh : '%d emahora',
            d : 'lilanga',
            dd : '%d emalanga',
            M : 'inyanga',
            MM : '%d tinyanga',
            y : 'umnyaka',
            yy : '%d iminyaka'
        },
        meridiemParse: /ekuseni|emini|entsambama|ebusuku/,
        meridiem : function (hours, minutes, isLower) {
            if (hours < 11) {
                return 'ekuseni';
            } else if (hours < 15) {
                return 'emini';
            } else if (hours < 19) {
                return 'entsambama';
            } else {
                return 'ebusuku';
            }
        },
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'ekuseni') {
                return hour;
            } else if (meridiem === 'emini') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'entsambama' || meridiem === 'ebusuku') {
                if (hour === 0) {
                    return 0;
                }
                return hour + 12;
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}/,
        ordinal : '%d',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('sv', {
        months : 'januari_februari_mars_april_maj_juni_juli_augusti_september_oktober_november_december'.split('_'),
        monthsShort : 'jan_feb_mar_apr_maj_jun_jul_aug_sep_okt_nov_dec'.split('_'),
        weekdays : 'söndag_måndag_tisdag_onsdag_torsdag_fredag_lördag'.split('_'),
        weekdaysShort : 'sön_mån_tis_ons_tor_fre_lör'.split('_'),
        weekdaysMin : 'sö_må_ti_on_to_fr_lö'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY-MM-DD',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY [kl.] HH:mm',
            LLLL : 'dddd D MMMM YYYY [kl.] HH:mm',
            lll : 'D MMM YYYY HH:mm',
            llll : 'ddd D MMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Idag] LT',
            nextDay: '[Imorgon] LT',
            lastDay: '[Igår] LT',
            nextWeek: '[På] dddd LT',
            lastWeek: '[I] dddd[s] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'om %s',
            past : 'för %s sedan',
            s : 'några sekunder',
            ss : '%d sekunder',
            m : 'en minut',
            mm : '%d minuter',
            h : 'en timme',
            hh : '%d timmar',
            d : 'en dag',
            dd : '%d dagar',
            M : 'en månad',
            MM : '%d månader',
            y : 'ett år',
            yy : '%d år'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(e|a)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'e' :
                (b === 1) ? 'a' :
                (b === 2) ? 'a' :
                (b === 3) ? 'e' : 'e';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('sw', {
        months : 'Januari_Februari_Machi_Aprili_Mei_Juni_Julai_Agosti_Septemba_Oktoba_Novemba_Desemba'.split('_'),
        monthsShort : 'Jan_Feb_Mac_Apr_Mei_Jun_Jul_Ago_Sep_Okt_Nov_Des'.split('_'),
        weekdays : 'Jumapili_Jumatatu_Jumanne_Jumatano_Alhamisi_Ijumaa_Jumamosi'.split('_'),
        weekdaysShort : 'Jpl_Jtat_Jnne_Jtan_Alh_Ijm_Jmos'.split('_'),
        weekdaysMin : 'J2_J3_J4_J5_Al_Ij_J1'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[leo saa] LT',
            nextDay : '[kesho saa] LT',
            nextWeek : '[wiki ijayo] dddd [saat] LT',
            lastDay : '[jana] LT',
            lastWeek : '[wiki iliyopita] dddd [saat] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s baadaye',
            past : 'tokea %s',
            s : 'hivi punde',
            ss : 'sekunde %d',
            m : 'dakika moja',
            mm : 'dakika %d',
            h : 'saa limoja',
            hh : 'masaa %d',
            d : 'siku moja',
            dd : 'masiku %d',
            M : 'mwezi mmoja',
            MM : 'miezi %d',
            y : 'mwaka mmoja',
            yy : 'miaka %d'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var symbolMap$f = {
        '1': '௧',
        '2': '௨',
        '3': '௩',
        '4': '௪',
        '5': '௫',
        '6': '௬',
        '7': '௭',
        '8': '௮',
        '9': '௯',
        '0': '௦'
    }, numberMap$e = {
        '௧': '1',
        '௨': '2',
        '௩': '3',
        '௪': '4',
        '௫': '5',
        '௬': '6',
        '௭': '7',
        '௮': '8',
        '௯': '9',
        '௦': '0'
    };

    hooks.defineLocale('ta', {
        months : 'ஜனவரி_பிப்ரவரி_மார்ச்_ஏப்ரல்_மே_ஜூன்_ஜூலை_ஆகஸ்ட்_செப்டெம்பர்_அக்டோபர்_நவம்பர்_டிசம்பர்'.split('_'),
        monthsShort : 'ஜனவரி_பிப்ரவரி_மார்ச்_ஏப்ரல்_மே_ஜூன்_ஜூலை_ஆகஸ்ட்_செப்டெம்பர்_அக்டோபர்_நவம்பர்_டிசம்பர்'.split('_'),
        weekdays : 'ஞாயிற்றுக்கிழமை_திங்கட்கிழமை_செவ்வாய்கிழமை_புதன்கிழமை_வியாழக்கிழமை_வெள்ளிக்கிழமை_சனிக்கிழமை'.split('_'),
        weekdaysShort : 'ஞாயிறு_திங்கள்_செவ்வாய்_புதன்_வியாழன்_வெள்ளி_சனி'.split('_'),
        weekdaysMin : 'ஞா_தி_செ_பு_வி_வெ_ச'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, HH:mm',
            LLLL : 'dddd, D MMMM YYYY, HH:mm'
        },
        calendar : {
            sameDay : '[இன்று] LT',
            nextDay : '[நாளை] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[நேற்று] LT',
            lastWeek : '[கடந்த வாரம்] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s இல்',
            past : '%s முன்',
            s : 'ஒரு சில விநாடிகள்',
            ss : '%d விநாடிகள்',
            m : 'ஒரு நிமிடம்',
            mm : '%d நிமிடங்கள்',
            h : 'ஒரு மணி நேரம்',
            hh : '%d மணி நேரம்',
            d : 'ஒரு நாள்',
            dd : '%d நாட்கள்',
            M : 'ஒரு மாதம்',
            MM : '%d மாதங்கள்',
            y : 'ஒரு வருடம்',
            yy : '%d ஆண்டுகள்'
        },
        dayOfMonthOrdinalParse: /\d{1,2}வது/,
        ordinal : function (number) {
            return number + 'வது';
        },
        preparse: function (string) {
            return string.replace(/[௧௨௩௪௫௬௭௮௯௦]/g, function (match) {
                return numberMap$e[match];
            });
        },
        postformat: function (string) {
            return string.replace(/\d/g, function (match) {
                return symbolMap$f[match];
            });
        },
        // refer http://ta.wikipedia.org/s/1er1
        meridiemParse: /யாமம்|வைகறை|காலை|நண்பகல்|எற்பாடு|மாலை/,
        meridiem : function (hour, minute, isLower) {
            if (hour < 2) {
                return ' யாமம்';
            } else if (hour < 6) {
                return ' வைகறை';  // வைகறை
            } else if (hour < 10) {
                return ' காலை'; // காலை
            } else if (hour < 14) {
                return ' நண்பகல்'; // நண்பகல்
            } else if (hour < 18) {
                return ' எற்பாடு'; // எற்பாடு
            } else if (hour < 22) {
                return ' மாலை'; // மாலை
            } else {
                return ' யாமம்';
            }
        },
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'யாமம்') {
                return hour < 2 ? hour : hour + 12;
            } else if (meridiem === 'வைகறை' || meridiem === 'காலை') {
                return hour;
            } else if (meridiem === 'நண்பகல்') {
                return hour >= 10 ? hour : hour + 12;
            } else {
                return hour + 12;
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('te', {
        months : 'జనవరి_ఫిబ్రవరి_మార్చి_ఏప్రిల్_మే_జూన్_జులై_ఆగస్టు_సెప్టెంబర్_అక్టోబర్_నవంబర్_డిసెంబర్'.split('_'),
        monthsShort : 'జన._ఫిబ్ర._మార్చి_ఏప్రి._మే_జూన్_జులై_ఆగ._సెప్._అక్టో._నవ._డిసె.'.split('_'),
        monthsParseExact : true,
        weekdays : 'ఆదివారం_సోమవారం_మంగళవారం_బుధవారం_గురువారం_శుక్రవారం_శనివారం'.split('_'),
        weekdaysShort : 'ఆది_సోమ_మంగళ_బుధ_గురు_శుక్ర_శని'.split('_'),
        weekdaysMin : 'ఆ_సో_మం_బు_గు_శు_శ'.split('_'),
        longDateFormat : {
            LT : 'A h:mm',
            LTS : 'A h:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY, A h:mm',
            LLLL : 'dddd, D MMMM YYYY, A h:mm'
        },
        calendar : {
            sameDay : '[నేడు] LT',
            nextDay : '[రేపు] LT',
            nextWeek : 'dddd, LT',
            lastDay : '[నిన్న] LT',
            lastWeek : '[గత] dddd, LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s లో',
            past : '%s క్రితం',
            s : 'కొన్ని క్షణాలు',
            ss : '%d సెకన్లు',
            m : 'ఒక నిమిషం',
            mm : '%d నిమిషాలు',
            h : 'ఒక గంట',
            hh : '%d గంటలు',
            d : 'ఒక రోజు',
            dd : '%d రోజులు',
            M : 'ఒక నెల',
            MM : '%d నెలలు',
            y : 'ఒక సంవత్సరం',
            yy : '%d సంవత్సరాలు'
        },
        dayOfMonthOrdinalParse : /\d{1,2}వ/,
        ordinal : '%dవ',
        meridiemParse: /రాత్రి|ఉదయం|మధ్యాహ్నం|సాయంత్రం/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'రాత్రి') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'ఉదయం') {
                return hour;
            } else if (meridiem === 'మధ్యాహ్నం') {
                return hour >= 10 ? hour : hour + 12;
            } else if (meridiem === 'సాయంత్రం') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'రాత్రి';
            } else if (hour < 10) {
                return 'ఉదయం';
            } else if (hour < 17) {
                return 'మధ్యాహ్నం';
            } else if (hour < 20) {
                return 'సాయంత్రం';
            } else {
                return 'రాత్రి';
            }
        },
        week : {
            dow : 0, // Sunday is the first day of the week.
            doy : 6  // The week that contains Jan 6th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('tet', {
        months : 'Janeiru_Fevereiru_Marsu_Abril_Maiu_Juñu_Jullu_Agustu_Setembru_Outubru_Novembru_Dezembru'.split('_'),
        monthsShort : 'Jan_Fev_Mar_Abr_Mai_Jun_Jul_Ago_Set_Out_Nov_Dez'.split('_'),
        weekdays : 'Domingu_Segunda_Tersa_Kuarta_Kinta_Sesta_Sabadu'.split('_'),
        weekdaysShort : 'Dom_Seg_Ters_Kua_Kint_Sest_Sab'.split('_'),
        weekdaysMin : 'Do_Seg_Te_Ku_Ki_Ses_Sa'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Ohin iha] LT',
            nextDay: '[Aban iha] LT',
            nextWeek: 'dddd [iha] LT',
            lastDay: '[Horiseik iha] LT',
            lastWeek: 'dddd [semana kotuk] [iha] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'iha %s',
            past : '%s liuba',
            s : 'minutu balun',
            ss : 'minutu %d',
            m : 'minutu ida',
            mm : 'minutu %d',
            h : 'oras ida',
            hh : 'oras %d',
            d : 'loron ida',
            dd : 'loron %d',
            M : 'fulan ida',
            MM : 'fulan %d',
            y : 'tinan ida',
            yy : 'tinan %d'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(st|nd|rd|th)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var suffixes$3 = {
        0: '-ум',
        1: '-ум',
        2: '-юм',
        3: '-юм',
        4: '-ум',
        5: '-ум',
        6: '-ум',
        7: '-ум',
        8: '-ум',
        9: '-ум',
        10: '-ум',
        12: '-ум',
        13: '-ум',
        20: '-ум',
        30: '-юм',
        40: '-ум',
        50: '-ум',
        60: '-ум',
        70: '-ум',
        80: '-ум',
        90: '-ум',
        100: '-ум'
    };

    hooks.defineLocale('tg', {
        months : 'январ_феврал_март_апрел_май_июн_июл_август_сентябр_октябр_ноябр_декабр'.split('_'),
        monthsShort : 'янв_фев_мар_апр_май_июн_июл_авг_сен_окт_ноя_дек'.split('_'),
        weekdays : 'якшанбе_душанбе_сешанбе_чоршанбе_панҷшанбе_ҷумъа_шанбе'.split('_'),
        weekdaysShort : 'яшб_дшб_сшб_чшб_пшб_ҷум_шнб'.split('_'),
        weekdaysMin : 'яш_дш_сш_чш_пш_ҷм_шб'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[Имрӯз соати] LT',
            nextDay : '[Пагоҳ соати] LT',
            lastDay : '[Дирӯз соати] LT',
            nextWeek : 'dddd[и] [ҳафтаи оянда соати] LT',
            lastWeek : 'dddd[и] [ҳафтаи гузашта соати] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'баъди %s',
            past : '%s пеш',
            s : 'якчанд сония',
            m : 'як дақиқа',
            mm : '%d дақиқа',
            h : 'як соат',
            hh : '%d соат',
            d : 'як рӯз',
            dd : '%d рӯз',
            M : 'як моҳ',
            MM : '%d моҳ',
            y : 'як сол',
            yy : '%d сол'
        },
        meridiemParse: /шаб|субҳ|рӯз|бегоҳ/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === 'шаб') {
                return hour < 4 ? hour : hour + 12;
            } else if (meridiem === 'субҳ') {
                return hour;
            } else if (meridiem === 'рӯз') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === 'бегоҳ') {
                return hour + 12;
            }
        },
        meridiem: function (hour, minute, isLower) {
            if (hour < 4) {
                return 'шаб';
            } else if (hour < 11) {
                return 'субҳ';
            } else if (hour < 16) {
                return 'рӯз';
            } else if (hour < 19) {
                return 'бегоҳ';
            } else {
                return 'шаб';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(ум|юм)/,
        ordinal: function (number) {
            var a = number % 10,
                b = number >= 100 ? 100 : null;
            return number + (suffixes$3[number] || suffixes$3[a] || suffixes$3[b]);
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 1th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('th', {
        months : 'มกราคม_กุมภาพันธ์_มีนาคม_เมษายน_พฤษภาคม_มิถุนายน_กรกฎาคม_สิงหาคม_กันยายน_ตุลาคม_พฤศจิกายน_ธันวาคม'.split('_'),
        monthsShort : 'ม.ค._ก.พ._มี.ค._เม.ย._พ.ค._มิ.ย._ก.ค._ส.ค._ก.ย._ต.ค._พ.ย._ธ.ค.'.split('_'),
        monthsParseExact: true,
        weekdays : 'อาทิตย์_จันทร์_อังคาร_พุธ_พฤหัสบดี_ศุกร์_เสาร์'.split('_'),
        weekdaysShort : 'อาทิตย์_จันทร์_อังคาร_พุธ_พฤหัส_ศุกร์_เสาร์'.split('_'), // yes, three characters difference
        weekdaysMin : 'อา._จ._อ._พ._พฤ._ศ._ส.'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'H:mm',
            LTS : 'H:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY เวลา H:mm',
            LLLL : 'วันddddที่ D MMMM YYYY เวลา H:mm'
        },
        meridiemParse: /ก่อนเที่ยง|หลังเที่ยง/,
        isPM: function (input) {
            return input === 'หลังเที่ยง';
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'ก่อนเที่ยง';
            } else {
                return 'หลังเที่ยง';
            }
        },
        calendar : {
            sameDay : '[วันนี้ เวลา] LT',
            nextDay : '[พรุ่งนี้ เวลา] LT',
            nextWeek : 'dddd[หน้า เวลา] LT',
            lastDay : '[เมื่อวานนี้ เวลา] LT',
            lastWeek : '[วัน]dddd[ที่แล้ว เวลา] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'อีก %s',
            past : '%sที่แล้ว',
            s : 'ไม่กี่วินาที',
            ss : '%d วินาที',
            m : '1 นาที',
            mm : '%d นาที',
            h : '1 ชั่วโมง',
            hh : '%d ชั่วโมง',
            d : '1 วัน',
            dd : '%d วัน',
            M : '1 เดือน',
            MM : '%d เดือน',
            y : '1 ปี',
            yy : '%d ปี'
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('tl-ph', {
        months : 'Enero_Pebrero_Marso_Abril_Mayo_Hunyo_Hulyo_Agosto_Setyembre_Oktubre_Nobyembre_Disyembre'.split('_'),
        monthsShort : 'Ene_Peb_Mar_Abr_May_Hun_Hul_Ago_Set_Okt_Nob_Dis'.split('_'),
        weekdays : 'Linggo_Lunes_Martes_Miyerkules_Huwebes_Biyernes_Sabado'.split('_'),
        weekdaysShort : 'Lin_Lun_Mar_Miy_Huw_Biy_Sab'.split('_'),
        weekdaysMin : 'Li_Lu_Ma_Mi_Hu_Bi_Sab'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'MM/D/YYYY',
            LL : 'MMMM D, YYYY',
            LLL : 'MMMM D, YYYY HH:mm',
            LLLL : 'dddd, MMMM DD, YYYY HH:mm'
        },
        calendar : {
            sameDay: 'LT [ngayong araw]',
            nextDay: '[Bukas ng] LT',
            nextWeek: 'LT [sa susunod na] dddd',
            lastDay: 'LT [kahapon]',
            lastWeek: 'LT [noong nakaraang] dddd',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'sa loob ng %s',
            past : '%s ang nakalipas',
            s : 'ilang segundo',
            ss : '%d segundo',
            m : 'isang minuto',
            mm : '%d minuto',
            h : 'isang oras',
            hh : '%d oras',
            d : 'isang araw',
            dd : '%d araw',
            M : 'isang buwan',
            MM : '%d buwan',
            y : 'isang taon',
            yy : '%d taon'
        },
        dayOfMonthOrdinalParse: /\d{1,2}/,
        ordinal : function (number) {
            return number;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var numbersNouns = 'pagh_wa’_cha’_wej_loS_vagh_jav_Soch_chorgh_Hut'.split('_');

    function translateFuture(output) {
        var time = output;
        time = (output.indexOf('jaj') !== -1) ?
        time.slice(0, -3) + 'leS' :
        (output.indexOf('jar') !== -1) ?
        time.slice(0, -3) + 'waQ' :
        (output.indexOf('DIS') !== -1) ?
        time.slice(0, -3) + 'nem' :
        time + ' pIq';
        return time;
    }

    function translatePast(output) {
        var time = output;
        time = (output.indexOf('jaj') !== -1) ?
        time.slice(0, -3) + 'Hu’' :
        (output.indexOf('jar') !== -1) ?
        time.slice(0, -3) + 'wen' :
        (output.indexOf('DIS') !== -1) ?
        time.slice(0, -3) + 'ben' :
        time + ' ret';
        return time;
    }

    function translate$a(number, withoutSuffix, string, isFuture) {
        var numberNoun = numberAsNoun(number);
        switch (string) {
            case 'ss':
                return numberNoun + ' lup';
            case 'mm':
                return numberNoun + ' tup';
            case 'hh':
                return numberNoun + ' rep';
            case 'dd':
                return numberNoun + ' jaj';
            case 'MM':
                return numberNoun + ' jar';
            case 'yy':
                return numberNoun + ' DIS';
        }
    }

    function numberAsNoun(number) {
        var hundred = Math.floor((number % 1000) / 100),
        ten = Math.floor((number % 100) / 10),
        one = number % 10,
        word = '';
        if (hundred > 0) {
            word += numbersNouns[hundred] + 'vatlh';
        }
        if (ten > 0) {
            word += ((word !== '') ? ' ' : '') + numbersNouns[ten] + 'maH';
        }
        if (one > 0) {
            word += ((word !== '') ? ' ' : '') + numbersNouns[one];
        }
        return (word === '') ? 'pagh' : word;
    }

    hooks.defineLocale('tlh', {
        months : 'tera’ jar wa’_tera’ jar cha’_tera’ jar wej_tera’ jar loS_tera’ jar vagh_tera’ jar jav_tera’ jar Soch_tera’ jar chorgh_tera’ jar Hut_tera’ jar wa’maH_tera’ jar wa’maH wa’_tera’ jar wa’maH cha’'.split('_'),
        monthsShort : 'jar wa’_jar cha’_jar wej_jar loS_jar vagh_jar jav_jar Soch_jar chorgh_jar Hut_jar wa’maH_jar wa’maH wa’_jar wa’maH cha’'.split('_'),
        monthsParseExact : true,
        weekdays : 'lojmItjaj_DaSjaj_povjaj_ghItlhjaj_loghjaj_buqjaj_ghInjaj'.split('_'),
        weekdaysShort : 'lojmItjaj_DaSjaj_povjaj_ghItlhjaj_loghjaj_buqjaj_ghInjaj'.split('_'),
        weekdaysMin : 'lojmItjaj_DaSjaj_povjaj_ghItlhjaj_loghjaj_buqjaj_ghInjaj'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[DaHjaj] LT',
            nextDay: '[wa’leS] LT',
            nextWeek: 'LLL',
            lastDay: '[wa’Hu’] LT',
            lastWeek: 'LLL',
            sameElse: 'L'
        },
        relativeTime : {
            future : translateFuture,
            past : translatePast,
            s : 'puS lup',
            ss : translate$a,
            m : 'wa’ tup',
            mm : translate$a,
            h : 'wa’ rep',
            hh : translate$a,
            d : 'wa’ jaj',
            dd : translate$a,
            M : 'wa’ jar',
            MM : translate$a,
            y : 'wa’ DIS',
            yy : translate$a
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    var suffixes$4 = {
        1: '\'inci',
        5: '\'inci',
        8: '\'inci',
        70: '\'inci',
        80: '\'inci',
        2: '\'nci',
        7: '\'nci',
        20: '\'nci',
        50: '\'nci',
        3: '\'üncü',
        4: '\'üncü',
        100: '\'üncü',
        6: '\'ncı',
        9: '\'uncu',
        10: '\'uncu',
        30: '\'uncu',
        60: '\'ıncı',
        90: '\'ıncı'
    };

    hooks.defineLocale('tr', {
        months : 'Ocak_Şubat_Mart_Nisan_Mayıs_Haziran_Temmuz_Ağustos_Eylül_Ekim_Kasım_Aralık'.split('_'),
        monthsShort : 'Oca_Şub_Mar_Nis_May_Haz_Tem_Ağu_Eyl_Eki_Kas_Ara'.split('_'),
        weekdays : 'Pazar_Pazartesi_Salı_Çarşamba_Perşembe_Cuma_Cumartesi'.split('_'),
        weekdaysShort : 'Paz_Pts_Sal_Çar_Per_Cum_Cts'.split('_'),
        weekdaysMin : 'Pz_Pt_Sa_Ça_Pe_Cu_Ct'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[bugün saat] LT',
            nextDay : '[yarın saat] LT',
            nextWeek : '[gelecek] dddd [saat] LT',
            lastDay : '[dün] LT',
            lastWeek : '[geçen] dddd [saat] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s sonra',
            past : '%s önce',
            s : 'birkaç saniye',
            ss : '%d saniye',
            m : 'bir dakika',
            mm : '%d dakika',
            h : 'bir saat',
            hh : '%d saat',
            d : 'bir gün',
            dd : '%d gün',
            M : 'bir ay',
            MM : '%d ay',
            y : 'bir yıl',
            yy : '%d yıl'
        },
        ordinal: function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'Do':
                case 'DD':
                    return number;
                default:
                    if (number === 0) {  // special case for zero
                        return number + '\'ıncı';
                    }
                    var a = number % 10,
                        b = number % 100 - a,
                        c = number >= 100 ? 100 : null;
                    return number + (suffixes$4[a] || suffixes$4[b] || suffixes$4[c]);
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    // After the year there should be a slash and the amount of years since December 26, 1979 in Roman numerals.
    // This is currently too difficult (maybe even impossible) to add.
    hooks.defineLocale('tzl', {
        months : 'Januar_Fevraglh_Març_Avrïu_Mai_Gün_Julia_Guscht_Setemvar_Listopäts_Noemvar_Zecemvar'.split('_'),
        monthsShort : 'Jan_Fev_Mar_Avr_Mai_Gün_Jul_Gus_Set_Lis_Noe_Zec'.split('_'),
        weekdays : 'Súladi_Lúneçi_Maitzi_Márcuri_Xhúadi_Viénerçi_Sáturi'.split('_'),
        weekdaysShort : 'Súl_Lún_Mai_Már_Xhú_Vié_Sát'.split('_'),
        weekdaysMin : 'Sú_Lú_Ma_Má_Xh_Vi_Sá'.split('_'),
        longDateFormat : {
            LT : 'HH.mm',
            LTS : 'HH.mm.ss',
            L : 'DD.MM.YYYY',
            LL : 'D. MMMM [dallas] YYYY',
            LLL : 'D. MMMM [dallas] YYYY HH.mm',
            LLLL : 'dddd, [li] D. MMMM [dallas] YYYY HH.mm'
        },
        meridiemParse: /d\'o|d\'a/i,
        isPM : function (input) {
            return 'd\'o' === input.toLowerCase();
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours > 11) {
                return isLower ? 'd\'o' : 'D\'O';
            } else {
                return isLower ? 'd\'a' : 'D\'A';
            }
        },
        calendar : {
            sameDay : '[oxhi à] LT',
            nextDay : '[demà à] LT',
            nextWeek : 'dddd [à] LT',
            lastDay : '[ieiri à] LT',
            lastWeek : '[sür el] dddd [lasteu à] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'osprei %s',
            past : 'ja%s',
            s : processRelativeTime$7,
            ss : processRelativeTime$7,
            m : processRelativeTime$7,
            mm : processRelativeTime$7,
            h : processRelativeTime$7,
            hh : processRelativeTime$7,
            d : processRelativeTime$7,
            dd : processRelativeTime$7,
            M : processRelativeTime$7,
            MM : processRelativeTime$7,
            y : processRelativeTime$7,
            yy : processRelativeTime$7
        },
        dayOfMonthOrdinalParse: /\d{1,2}\./,
        ordinal : '%d.',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    function processRelativeTime$7(number, withoutSuffix, key, isFuture) {
        var format = {
            's': ['viensas secunds', '\'iensas secunds'],
            'ss': [number + ' secunds', '' + number + ' secunds'],
            'm': ['\'n míut', '\'iens míut'],
            'mm': [number + ' míuts', '' + number + ' míuts'],
            'h': ['\'n þora', '\'iensa þora'],
            'hh': [number + ' þoras', '' + number + ' þoras'],
            'd': ['\'n ziua', '\'iensa ziua'],
            'dd': [number + ' ziuas', '' + number + ' ziuas'],
            'M': ['\'n mes', '\'iens mes'],
            'MM': [number + ' mesen', '' + number + ' mesen'],
            'y': ['\'n ar', '\'iens ar'],
            'yy': [number + ' ars', '' + number + ' ars']
        };
        return isFuture ? format[key][0] : (withoutSuffix ? format[key][0] : format[key][1]);
    }

    //! moment.js locale configuration

    hooks.defineLocale('tzm-latn', {
        months : 'innayr_brˤayrˤ_marˤsˤ_ibrir_mayyw_ywnyw_ywlywz_ɣwšt_šwtanbir_ktˤwbrˤ_nwwanbir_dwjnbir'.split('_'),
        monthsShort : 'innayr_brˤayrˤ_marˤsˤ_ibrir_mayyw_ywnyw_ywlywz_ɣwšt_šwtanbir_ktˤwbrˤ_nwwanbir_dwjnbir'.split('_'),
        weekdays : 'asamas_aynas_asinas_akras_akwas_asimwas_asiḍyas'.split('_'),
        weekdaysShort : 'asamas_aynas_asinas_akras_akwas_asimwas_asiḍyas'.split('_'),
        weekdaysMin : 'asamas_aynas_asinas_akras_akwas_asimwas_asiḍyas'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[asdkh g] LT',
            nextDay: '[aska g] LT',
            nextWeek: 'dddd [g] LT',
            lastDay: '[assant g] LT',
            lastWeek: 'dddd [g] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'dadkh s yan %s',
            past : 'yan %s',
            s : 'imik',
            ss : '%d imik',
            m : 'minuḍ',
            mm : '%d minuḍ',
            h : 'saɛa',
            hh : '%d tassaɛin',
            d : 'ass',
            dd : '%d ossan',
            M : 'ayowr',
            MM : '%d iyyirn',
            y : 'asgas',
            yy : '%d isgasn'
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('tzm', {
        months : 'ⵉⵏⵏⴰⵢⵔ_ⴱⵕⴰⵢⵕ_ⵎⴰⵕⵚ_ⵉⴱⵔⵉⵔ_ⵎⴰⵢⵢⵓ_ⵢⵓⵏⵢⵓ_ⵢⵓⵍⵢⵓⵣ_ⵖⵓⵛⵜ_ⵛⵓⵜⴰⵏⴱⵉⵔ_ⴽⵟⵓⴱⵕ_ⵏⵓⵡⴰⵏⴱⵉⵔ_ⴷⵓⵊⵏⴱⵉⵔ'.split('_'),
        monthsShort : 'ⵉⵏⵏⴰⵢⵔ_ⴱⵕⴰⵢⵕ_ⵎⴰⵕⵚ_ⵉⴱⵔⵉⵔ_ⵎⴰⵢⵢⵓ_ⵢⵓⵏⵢⵓ_ⵢⵓⵍⵢⵓⵣ_ⵖⵓⵛⵜ_ⵛⵓⵜⴰⵏⴱⵉⵔ_ⴽⵟⵓⴱⵕ_ⵏⵓⵡⴰⵏⴱⵉⵔ_ⴷⵓⵊⵏⴱⵉⵔ'.split('_'),
        weekdays : 'ⴰⵙⴰⵎⴰⵙ_ⴰⵢⵏⴰⵙ_ⴰⵙⵉⵏⴰⵙ_ⴰⴽⵔⴰⵙ_ⴰⴽⵡⴰⵙ_ⴰⵙⵉⵎⵡⴰⵙ_ⴰⵙⵉⴹⵢⴰⵙ'.split('_'),
        weekdaysShort : 'ⴰⵙⴰⵎⴰⵙ_ⴰⵢⵏⴰⵙ_ⴰⵙⵉⵏⴰⵙ_ⴰⴽⵔⴰⵙ_ⴰⴽⵡⴰⵙ_ⴰⵙⵉⵎⵡⴰⵙ_ⴰⵙⵉⴹⵢⴰⵙ'.split('_'),
        weekdaysMin : 'ⴰⵙⴰⵎⴰⵙ_ⴰⵢⵏⴰⵙ_ⴰⵙⵉⵏⴰⵙ_ⴰⴽⵔⴰⵙ_ⴰⴽⵡⴰⵙ_ⴰⵙⵉⵎⵡⴰⵙ_ⴰⵙⵉⴹⵢⴰⵙ'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS: 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[ⴰⵙⴷⵅ ⴴ] LT',
            nextDay: '[ⴰⵙⴽⴰ ⴴ] LT',
            nextWeek: 'dddd [ⴴ] LT',
            lastDay: '[ⴰⵚⴰⵏⵜ ⴴ] LT',
            lastWeek: 'dddd [ⴴ] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : 'ⴷⴰⴷⵅ ⵙ ⵢⴰⵏ %s',
            past : 'ⵢⴰⵏ %s',
            s : 'ⵉⵎⵉⴽ',
            ss : '%d ⵉⵎⵉⴽ',
            m : 'ⵎⵉⵏⵓⴺ',
            mm : '%d ⵎⵉⵏⵓⴺ',
            h : 'ⵙⴰⵄⴰ',
            hh : '%d ⵜⴰⵙⵙⴰⵄⵉⵏ',
            d : 'ⴰⵙⵙ',
            dd : '%d oⵙⵙⴰⵏ',
            M : 'ⴰⵢoⵓⵔ',
            MM : '%d ⵉⵢⵢⵉⵔⵏ',
            y : 'ⴰⵙⴳⴰⵙ',
            yy : '%d ⵉⵙⴳⴰⵙⵏ'
        },
        week : {
            dow : 6, // Saturday is the first day of the week.
            doy : 12  // The week that contains Jan 12th is the first week of the year.
        }
    });

    //! moment.js language configuration

    hooks.defineLocale('ug-cn', {
        months: 'يانۋار_فېۋرال_مارت_ئاپرېل_ماي_ئىيۇن_ئىيۇل_ئاۋغۇست_سېنتەبىر_ئۆكتەبىر_نويابىر_دېكابىر'.split(
            '_'
        ),
        monthsShort: 'يانۋار_فېۋرال_مارت_ئاپرېل_ماي_ئىيۇن_ئىيۇل_ئاۋغۇست_سېنتەبىر_ئۆكتەبىر_نويابىر_دېكابىر'.split(
            '_'
        ),
        weekdays: 'يەكشەنبە_دۈشەنبە_سەيشەنبە_چارشەنبە_پەيشەنبە_جۈمە_شەنبە'.split(
            '_'
        ),
        weekdaysShort: 'يە_دۈ_سە_چا_پە_جۈ_شە'.split('_'),
        weekdaysMin: 'يە_دۈ_سە_چا_پە_جۈ_شە'.split('_'),
        longDateFormat: {
            LT: 'HH:mm',
            LTS: 'HH:mm:ss',
            L: 'YYYY-MM-DD',
            LL: 'YYYY-يىلىM-ئاينىڭD-كۈنى',
            LLL: 'YYYY-يىلىM-ئاينىڭD-كۈنى، HH:mm',
            LLLL: 'dddd، YYYY-يىلىM-ئاينىڭD-كۈنى، HH:mm'
        },
        meridiemParse: /يېرىم كېچە|سەھەر|چۈشتىن بۇرۇن|چۈش|چۈشتىن كېيىن|كەچ/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (
                meridiem === 'يېرىم كېچە' ||
                meridiem === 'سەھەر' ||
                meridiem === 'چۈشتىن بۇرۇن'
            ) {
                return hour;
            } else if (meridiem === 'چۈشتىن كېيىن' || meridiem === 'كەچ') {
                return hour + 12;
            } else {
                return hour >= 11 ? hour : hour + 12;
            }
        },
        meridiem: function (hour, minute, isLower) {
            var hm = hour * 100 + minute;
            if (hm < 600) {
                return 'يېرىم كېچە';
            } else if (hm < 900) {
                return 'سەھەر';
            } else if (hm < 1130) {
                return 'چۈشتىن بۇرۇن';
            } else if (hm < 1230) {
                return 'چۈش';
            } else if (hm < 1800) {
                return 'چۈشتىن كېيىن';
            } else {
                return 'كەچ';
            }
        },
        calendar: {
            sameDay: '[بۈگۈن سائەت] LT',
            nextDay: '[ئەتە سائەت] LT',
            nextWeek: '[كېلەركى] dddd [سائەت] LT',
            lastDay: '[تۆنۈگۈن] LT',
            lastWeek: '[ئالدىنقى] dddd [سائەت] LT',
            sameElse: 'L'
        },
        relativeTime: {
            future: '%s كېيىن',
            past: '%s بۇرۇن',
            s: 'نەچچە سېكونت',
            ss: '%d سېكونت',
            m: 'بىر مىنۇت',
            mm: '%d مىنۇت',
            h: 'بىر سائەت',
            hh: '%d سائەت',
            d: 'بىر كۈن',
            dd: '%d كۈن',
            M: 'بىر ئاي',
            MM: '%d ئاي',
            y: 'بىر يىل',
            yy: '%d يىل'
        },

        dayOfMonthOrdinalParse: /\d{1,2}(-كۈنى|-ئاي|-ھەپتە)/,
        ordinal: function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'DDD':
                    return number + '-كۈنى';
                case 'w':
                case 'W':
                    return number + '-ھەپتە';
                default:
                    return number;
            }
        },
        preparse: function (string) {
            return string.replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/,/g, '،');
        },
        week: {
            // GB/T 7408-1994《数据元和交换格式·信息交换·日期和时间表示法》与ISO 8601:1988等效
            dow: 1, // Monday is the first day of the week.
            doy: 7 // The week that contains Jan 1st is the first week of the year.
        }
    });

    //! moment.js locale configuration

    function plural$6(word, num) {
        var forms = word.split('_');
        return num % 10 === 1 && num % 100 !== 11 ? forms[0] : (num % 10 >= 2 && num % 10 <= 4 && (num % 100 < 10 || num % 100 >= 20) ? forms[1] : forms[2]);
    }
    function relativeTimeWithPlural$4(number, withoutSuffix, key) {
        var format = {
            'ss': withoutSuffix ? 'секунда_секунди_секунд' : 'секунду_секунди_секунд',
            'mm': withoutSuffix ? 'хвилина_хвилини_хвилин' : 'хвилину_хвилини_хвилин',
            'hh': withoutSuffix ? 'година_години_годин' : 'годину_години_годин',
            'dd': 'день_дні_днів',
            'MM': 'місяць_місяці_місяців',
            'yy': 'рік_роки_років'
        };
        if (key === 'm') {
            return withoutSuffix ? 'хвилина' : 'хвилину';
        }
        else if (key === 'h') {
            return withoutSuffix ? 'година' : 'годину';
        }
        else {
            return number + ' ' + plural$6(format[key], +number);
        }
    }
    function weekdaysCaseReplace(m, format) {
        var weekdays = {
            'nominative': 'неділя_понеділок_вівторок_середа_четвер_п’ятниця_субота'.split('_'),
            'accusative': 'неділю_понеділок_вівторок_середу_четвер_п’ятницю_суботу'.split('_'),
            'genitive': 'неділі_понеділка_вівторка_середи_четверга_п’ятниці_суботи'.split('_')
        };

        if (m === true) {
            return weekdays['nominative'].slice(1, 7).concat(weekdays['nominative'].slice(0, 1));
        }
        if (!m) {
            return weekdays['nominative'];
        }

        var nounCase = (/(\[[ВвУу]\]) ?dddd/).test(format) ?
            'accusative' :
            ((/\[?(?:минулої|наступної)? ?\] ?dddd/).test(format) ?
                'genitive' :
                'nominative');
        return weekdays[nounCase][m.day()];
    }
    function processHoursFunction(str) {
        return function () {
            return str + 'о' + (this.hours() === 11 ? 'б' : '') + '] LT';
        };
    }

    hooks.defineLocale('uk', {
        months : {
            'format': 'січня_лютого_березня_квітня_травня_червня_липня_серпня_вересня_жовтня_листопада_грудня'.split('_'),
            'standalone': 'січень_лютий_березень_квітень_травень_червень_липень_серпень_вересень_жовтень_листопад_грудень'.split('_')
        },
        monthsShort : 'січ_лют_бер_квіт_трав_черв_лип_серп_вер_жовт_лист_груд'.split('_'),
        weekdays : weekdaysCaseReplace,
        weekdaysShort : 'нд_пн_вт_ср_чт_пт_сб'.split('_'),
        weekdaysMin : 'нд_пн_вт_ср_чт_пт_сб'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD.MM.YYYY',
            LL : 'D MMMM YYYY р.',
            LLL : 'D MMMM YYYY р., HH:mm',
            LLLL : 'dddd, D MMMM YYYY р., HH:mm'
        },
        calendar : {
            sameDay: processHoursFunction('[Сьогодні '),
            nextDay: processHoursFunction('[Завтра '),
            lastDay: processHoursFunction('[Вчора '),
            nextWeek: processHoursFunction('[У] dddd ['),
            lastWeek: function () {
                switch (this.day()) {
                    case 0:
                    case 3:
                    case 5:
                    case 6:
                        return processHoursFunction('[Минулої] dddd [').call(this);
                    case 1:
                    case 2:
                    case 4:
                        return processHoursFunction('[Минулого] dddd [').call(this);
                }
            },
            sameElse: 'L'
        },
        relativeTime : {
            future : 'за %s',
            past : '%s тому',
            s : 'декілька секунд',
            ss : relativeTimeWithPlural$4,
            m : relativeTimeWithPlural$4,
            mm : relativeTimeWithPlural$4,
            h : 'годину',
            hh : relativeTimeWithPlural$4,
            d : 'день',
            dd : relativeTimeWithPlural$4,
            M : 'місяць',
            MM : relativeTimeWithPlural$4,
            y : 'рік',
            yy : relativeTimeWithPlural$4
        },
        // M. E.: those two are virtually unused but a user might want to implement them for his/her website for some reason
        meridiemParse: /ночі|ранку|дня|вечора/,
        isPM: function (input) {
            return /^(дня|вечора)$/.test(input);
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 4) {
                return 'ночі';
            } else if (hour < 12) {
                return 'ранку';
            } else if (hour < 17) {
                return 'дня';
            } else {
                return 'вечора';
            }
        },
        dayOfMonthOrdinalParse: /\d{1,2}-(й|го)/,
        ordinal: function (number, period) {
            switch (period) {
                case 'M':
                case 'd':
                case 'DDD':
                case 'w':
                case 'W':
                    return number + '-й';
                case 'D':
                    return number + '-го';
                default:
                    return number;
            }
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    var months$a = [
        'جنوری',
        'فروری',
        'مارچ',
        'اپریل',
        'مئی',
        'جون',
        'جولائی',
        'اگست',
        'ستمبر',
        'اکتوبر',
        'نومبر',
        'دسمبر'
    ];
    var days$2 = [
        'اتوار',
        'پیر',
        'منگل',
        'بدھ',
        'جمعرات',
        'جمعہ',
        'ہفتہ'
    ];

    hooks.defineLocale('ur', {
        months : months$a,
        monthsShort : months$a,
        weekdays : days$2,
        weekdaysShort : days$2,
        weekdaysMin : days$2,
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd، D MMMM YYYY HH:mm'
        },
        meridiemParse: /صبح|شام/,
        isPM : function (input) {
            return 'شام' === input;
        },
        meridiem : function (hour, minute, isLower) {
            if (hour < 12) {
                return 'صبح';
            }
            return 'شام';
        },
        calendar : {
            sameDay : '[آج بوقت] LT',
            nextDay : '[کل بوقت] LT',
            nextWeek : 'dddd [بوقت] LT',
            lastDay : '[گذشتہ روز بوقت] LT',
            lastWeek : '[گذشتہ] dddd [بوقت] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : '%s بعد',
            past : '%s قبل',
            s : 'چند سیکنڈ',
            ss : '%d سیکنڈ',
            m : 'ایک منٹ',
            mm : '%d منٹ',
            h : 'ایک گھنٹہ',
            hh : '%d گھنٹے',
            d : 'ایک دن',
            dd : '%d دن',
            M : 'ایک ماہ',
            MM : '%d ماہ',
            y : 'ایک سال',
            yy : '%d سال'
        },
        preparse: function (string) {
            return string.replace(/،/g, ',');
        },
        postformat: function (string) {
            return string.replace(/,/g, '،');
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('uz-latn', {
        months : 'Yanvar_Fevral_Mart_Aprel_May_Iyun_Iyul_Avgust_Sentabr_Oktabr_Noyabr_Dekabr'.split('_'),
        monthsShort : 'Yan_Fev_Mar_Apr_May_Iyun_Iyul_Avg_Sen_Okt_Noy_Dek'.split('_'),
        weekdays : 'Yakshanba_Dushanba_Seshanba_Chorshanba_Payshanba_Juma_Shanba'.split('_'),
        weekdaysShort : 'Yak_Dush_Sesh_Chor_Pay_Jum_Shan'.split('_'),
        weekdaysMin : 'Ya_Du_Se_Cho_Pa_Ju_Sha'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'D MMMM YYYY, dddd HH:mm'
        },
        calendar : {
            sameDay : '[Bugun soat] LT [da]',
            nextDay : '[Ertaga] LT [da]',
            nextWeek : 'dddd [kuni soat] LT [da]',
            lastDay : '[Kecha soat] LT [da]',
            lastWeek : '[O\'tgan] dddd [kuni soat] LT [da]',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'Yaqin %s ichida',
            past : 'Bir necha %s oldin',
            s : 'soniya',
            ss : '%d soniya',
            m : 'bir daqiqa',
            mm : '%d daqiqa',
            h : 'bir soat',
            hh : '%d soat',
            d : 'bir kun',
            dd : '%d kun',
            M : 'bir oy',
            MM : '%d oy',
            y : 'bir yil',
            yy : '%d yil'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 7th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('uz', {
        months : 'январ_феврал_март_апрел_май_июн_июл_август_сентябр_октябр_ноябр_декабр'.split('_'),
        monthsShort : 'янв_фев_мар_апр_май_июн_июл_авг_сен_окт_ноя_дек'.split('_'),
        weekdays : 'Якшанба_Душанба_Сешанба_Чоршанба_Пайшанба_Жума_Шанба'.split('_'),
        weekdaysShort : 'Якш_Душ_Сеш_Чор_Пай_Жум_Шан'.split('_'),
        weekdaysMin : 'Як_Ду_Се_Чо_Па_Жу_Ша'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'D MMMM YYYY, dddd HH:mm'
        },
        calendar : {
            sameDay : '[Бугун соат] LT [да]',
            nextDay : '[Эртага] LT [да]',
            nextWeek : 'dddd [куни соат] LT [да]',
            lastDay : '[Кеча соат] LT [да]',
            lastWeek : '[Утган] dddd [куни соат] LT [да]',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'Якин %s ичида',
            past : 'Бир неча %s олдин',
            s : 'фурсат',
            ss : '%d фурсат',
            m : 'бир дакика',
            mm : '%d дакика',
            h : 'бир соат',
            hh : '%d соат',
            d : 'бир кун',
            dd : '%d кун',
            M : 'бир ой',
            MM : '%d ой',
            y : 'бир йил',
            yy : '%d йил'
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 7  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('vi', {
        months : 'tháng 1_tháng 2_tháng 3_tháng 4_tháng 5_tháng 6_tháng 7_tháng 8_tháng 9_tháng 10_tháng 11_tháng 12'.split('_'),
        monthsShort : 'Th01_Th02_Th03_Th04_Th05_Th06_Th07_Th08_Th09_Th10_Th11_Th12'.split('_'),
        monthsParseExact : true,
        weekdays : 'chủ nhật_thứ hai_thứ ba_thứ tư_thứ năm_thứ sáu_thứ bảy'.split('_'),
        weekdaysShort : 'CN_T2_T3_T4_T5_T6_T7'.split('_'),
        weekdaysMin : 'CN_T2_T3_T4_T5_T6_T7'.split('_'),
        weekdaysParseExact : true,
        meridiemParse: /sa|ch/i,
        isPM : function (input) {
            return /^ch$/i.test(input);
        },
        meridiem : function (hours, minutes, isLower) {
            if (hours < 12) {
                return isLower ? 'sa' : 'SA';
            } else {
                return isLower ? 'ch' : 'CH';
            }
        },
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM [năm] YYYY',
            LLL : 'D MMMM [năm] YYYY HH:mm',
            LLLL : 'dddd, D MMMM [năm] YYYY HH:mm',
            l : 'DD/M/YYYY',
            ll : 'D MMM YYYY',
            lll : 'D MMM YYYY HH:mm',
            llll : 'ddd, D MMM YYYY HH:mm'
        },
        calendar : {
            sameDay: '[Hôm nay lúc] LT',
            nextDay: '[Ngày mai lúc] LT',
            nextWeek: 'dddd [tuần tới lúc] LT',
            lastDay: '[Hôm qua lúc] LT',
            lastWeek: 'dddd [tuần rồi lúc] LT',
            sameElse: 'L'
        },
        relativeTime : {
            future : '%s tới',
            past : '%s trước',
            s : 'vài giây',
            ss : '%d giây' ,
            m : 'một phút',
            mm : '%d phút',
            h : 'một giờ',
            hh : '%d giờ',
            d : 'một ngày',
            dd : '%d ngày',
            M : 'một tháng',
            MM : '%d tháng',
            y : 'một năm',
            yy : '%d năm'
        },
        dayOfMonthOrdinalParse: /\d{1,2}/,
        ordinal : function (number) {
            return number;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('x-pseudo', {
        months : 'J~áñúá~rý_F~ébrú~árý_~Márc~h_Áp~ríl_~Máý_~Júñé~_Júl~ý_Áú~gúst~_Sép~témb~ér_Ó~ctób~ér_Ñ~óvém~bér_~Décé~mbér'.split('_'),
        monthsShort : 'J~áñ_~Féb_~Már_~Ápr_~Máý_~Júñ_~Júl_~Áúg_~Sép_~Óct_~Ñóv_~Déc'.split('_'),
        monthsParseExact : true,
        weekdays : 'S~úñdá~ý_Mó~ñdáý~_Túé~sdáý~_Wéd~ñésd~áý_T~húrs~dáý_~Fríd~áý_S~átúr~dáý'.split('_'),
        weekdaysShort : 'S~úñ_~Móñ_~Túé_~Wéd_~Thú_~Frí_~Sát'.split('_'),
        weekdaysMin : 'S~ú_Mó~_Tú_~Wé_T~h_Fr~_Sá'.split('_'),
        weekdaysParseExact : true,
        longDateFormat : {
            LT : 'HH:mm',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY HH:mm',
            LLLL : 'dddd, D MMMM YYYY HH:mm'
        },
        calendar : {
            sameDay : '[T~ódá~ý át] LT',
            nextDay : '[T~ómó~rró~w át] LT',
            nextWeek : 'dddd [át] LT',
            lastDay : '[Ý~ést~érdá~ý át] LT',
            lastWeek : '[L~ást] dddd [át] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'í~ñ %s',
            past : '%s á~gó',
            s : 'á ~féw ~sécó~ñds',
            ss : '%d s~écóñ~ds',
            m : 'á ~míñ~úté',
            mm : '%d m~íñú~tés',
            h : 'á~ñ hó~úr',
            hh : '%d h~óúrs',
            d : 'á ~dáý',
            dd : '%d d~áýs',
            M : 'á ~móñ~th',
            MM : '%d m~óñt~hs',
            y : 'á ~ýéár',
            yy : '%d ý~éárs'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(th|st|nd|rd)/,
        ordinal : function (number) {
            var b = number % 10,
                output = (~~(number % 100 / 10) === 1) ? 'th' :
                (b === 1) ? 'st' :
                (b === 2) ? 'nd' :
                (b === 3) ? 'rd' : 'th';
            return number + output;
        },
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('yo', {
        months : 'Sẹ́rẹ́_Èrèlè_Ẹrẹ̀nà_Ìgbé_Èbibi_Òkùdu_Agẹmo_Ògún_Owewe_Ọ̀wàrà_Bélú_Ọ̀pẹ̀̀'.split('_'),
        monthsShort : 'Sẹ́r_Èrl_Ẹrn_Ìgb_Èbi_Òkù_Agẹ_Ògú_Owe_Ọ̀wà_Bél_Ọ̀pẹ̀̀'.split('_'),
        weekdays : 'Àìkú_Ajé_Ìsẹ́gun_Ọjọ́rú_Ọjọ́bọ_Ẹtì_Àbámẹ́ta'.split('_'),
        weekdaysShort : 'Àìk_Ajé_Ìsẹ́_Ọjr_Ọjb_Ẹtì_Àbá'.split('_'),
        weekdaysMin : 'Àì_Aj_Ìs_Ọr_Ọb_Ẹt_Àb'.split('_'),
        longDateFormat : {
            LT : 'h:mm A',
            LTS : 'h:mm:ss A',
            L : 'DD/MM/YYYY',
            LL : 'D MMMM YYYY',
            LLL : 'D MMMM YYYY h:mm A',
            LLLL : 'dddd, D MMMM YYYY h:mm A'
        },
        calendar : {
            sameDay : '[Ònì ni] LT',
            nextDay : '[Ọ̀la ni] LT',
            nextWeek : 'dddd [Ọsẹ̀ tón\'bọ] [ni] LT',
            lastDay : '[Àna ni] LT',
            lastWeek : 'dddd [Ọsẹ̀ tólọ́] [ni] LT',
            sameElse : 'L'
        },
        relativeTime : {
            future : 'ní %s',
            past : '%s kọjá',
            s : 'ìsẹjú aayá die',
            ss :'aayá %d',
            m : 'ìsẹjú kan',
            mm : 'ìsẹjú %d',
            h : 'wákati kan',
            hh : 'wákati %d',
            d : 'ọjọ́ kan',
            dd : 'ọjọ́ %d',
            M : 'osù kan',
            MM : 'osù %d',
            y : 'ọdún kan',
            yy : 'ọdún %d'
        },
        dayOfMonthOrdinalParse : /ọjọ́\s\d{1,2}/,
        ordinal : 'ọjọ́ %d',
        week : {
            dow : 1, // Monday is the first day of the week.
            doy : 4 // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('zh-cn', {
        months : '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
        monthsShort : '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
        weekdays : '星期日_星期一_星期二_星期三_星期四_星期五_星期六'.split('_'),
        weekdaysShort : '周日_周一_周二_周三_周四_周五_周六'.split('_'),
        weekdaysMin : '日_一_二_三_四_五_六'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY/MM/DD',
            LL : 'YYYY年M月D日',
            LLL : 'YYYY年M月D日Ah点mm分',
            LLLL : 'YYYY年M月D日ddddAh点mm分',
            l : 'YYYY/M/D',
            ll : 'YYYY年M月D日',
            lll : 'YYYY年M月D日 HH:mm',
            llll : 'YYYY年M月D日dddd HH:mm'
        },
        meridiemParse: /凌晨|早上|上午|中午|下午|晚上/,
        meridiemHour: function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === '凌晨' || meridiem === '早上' ||
                    meridiem === '上午') {
                return hour;
            } else if (meridiem === '下午' || meridiem === '晚上') {
                return hour + 12;
            } else {
                // '中午'
                return hour >= 11 ? hour : hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            var hm = hour * 100 + minute;
            if (hm < 600) {
                return '凌晨';
            } else if (hm < 900) {
                return '早上';
            } else if (hm < 1130) {
                return '上午';
            } else if (hm < 1230) {
                return '中午';
            } else if (hm < 1800) {
                return '下午';
            } else {
                return '晚上';
            }
        },
        calendar : {
            sameDay : '[今天]LT',
            nextDay : '[明天]LT',
            nextWeek : '[下]ddddLT',
            lastDay : '[昨天]LT',
            lastWeek : '[上]ddddLT',
            sameElse : 'L'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(日|月|周)/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd':
                case 'D':
                case 'DDD':
                    return number + '日';
                case 'M':
                    return number + '月';
                case 'w':
                case 'W':
                    return number + '周';
                default:
                    return number;
            }
        },
        relativeTime : {
            future : '%s内',
            past : '%s前',
            s : '几秒',
            ss : '%d 秒',
            m : '1 分钟',
            mm : '%d 分钟',
            h : '1 小时',
            hh : '%d 小时',
            d : '1 天',
            dd : '%d 天',
            M : '1 个月',
            MM : '%d 个月',
            y : '1 年',
            yy : '%d 年'
        },
        week : {
            // GB/T 7408-1994《数据元和交换格式·信息交换·日期和时间表示法》与ISO 8601:1988等效
            dow : 1, // Monday is the first day of the week.
            doy : 4  // The week that contains Jan 4th is the first week of the year.
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('zh-hk', {
        months : '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
        monthsShort : '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
        weekdays : '星期日_星期一_星期二_星期三_星期四_星期五_星期六'.split('_'),
        weekdaysShort : '週日_週一_週二_週三_週四_週五_週六'.split('_'),
        weekdaysMin : '日_一_二_三_四_五_六'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY/MM/DD',
            LL : 'YYYY年M月D日',
            LLL : 'YYYY年M月D日 HH:mm',
            LLLL : 'YYYY年M月D日dddd HH:mm',
            l : 'YYYY/M/D',
            ll : 'YYYY年M月D日',
            lll : 'YYYY年M月D日 HH:mm',
            llll : 'YYYY年M月D日dddd HH:mm'
        },
        meridiemParse: /凌晨|早上|上午|中午|下午|晚上/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === '凌晨' || meridiem === '早上' || meridiem === '上午') {
                return hour;
            } else if (meridiem === '中午') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === '下午' || meridiem === '晚上') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            var hm = hour * 100 + minute;
            if (hm < 600) {
                return '凌晨';
            } else if (hm < 900) {
                return '早上';
            } else if (hm < 1130) {
                return '上午';
            } else if (hm < 1230) {
                return '中午';
            } else if (hm < 1800) {
                return '下午';
            } else {
                return '晚上';
            }
        },
        calendar : {
            sameDay : '[今天]LT',
            nextDay : '[明天]LT',
            nextWeek : '[下]ddddLT',
            lastDay : '[昨天]LT',
            lastWeek : '[上]ddddLT',
            sameElse : 'L'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(日|月|週)/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd' :
                case 'D' :
                case 'DDD' :
                    return number + '日';
                case 'M' :
                    return number + '月';
                case 'w' :
                case 'W' :
                    return number + '週';
                default :
                    return number;
            }
        },
        relativeTime : {
            future : '%s內',
            past : '%s前',
            s : '幾秒',
            ss : '%d 秒',
            m : '1 分鐘',
            mm : '%d 分鐘',
            h : '1 小時',
            hh : '%d 小時',
            d : '1 天',
            dd : '%d 天',
            M : '1 個月',
            MM : '%d 個月',
            y : '1 年',
            yy : '%d 年'
        }
    });

    //! moment.js locale configuration

    hooks.defineLocale('zh-tw', {
        months : '一月_二月_三月_四月_五月_六月_七月_八月_九月_十月_十一月_十二月'.split('_'),
        monthsShort : '1月_2月_3月_4月_5月_6月_7月_8月_9月_10月_11月_12月'.split('_'),
        weekdays : '星期日_星期一_星期二_星期三_星期四_星期五_星期六'.split('_'),
        weekdaysShort : '週日_週一_週二_週三_週四_週五_週六'.split('_'),
        weekdaysMin : '日_一_二_三_四_五_六'.split('_'),
        longDateFormat : {
            LT : 'HH:mm',
            LTS : 'HH:mm:ss',
            L : 'YYYY/MM/DD',
            LL : 'YYYY年M月D日',
            LLL : 'YYYY年M月D日 HH:mm',
            LLLL : 'YYYY年M月D日dddd HH:mm',
            l : 'YYYY/M/D',
            ll : 'YYYY年M月D日',
            lll : 'YYYY年M月D日 HH:mm',
            llll : 'YYYY年M月D日dddd HH:mm'
        },
        meridiemParse: /凌晨|早上|上午|中午|下午|晚上/,
        meridiemHour : function (hour, meridiem) {
            if (hour === 12) {
                hour = 0;
            }
            if (meridiem === '凌晨' || meridiem === '早上' || meridiem === '上午') {
                return hour;
            } else if (meridiem === '中午') {
                return hour >= 11 ? hour : hour + 12;
            } else if (meridiem === '下午' || meridiem === '晚上') {
                return hour + 12;
            }
        },
        meridiem : function (hour, minute, isLower) {
            var hm = hour * 100 + minute;
            if (hm < 600) {
                return '凌晨';
            } else if (hm < 900) {
                return '早上';
            } else if (hm < 1130) {
                return '上午';
            } else if (hm < 1230) {
                return '中午';
            } else if (hm < 1800) {
                return '下午';
            } else {
                return '晚上';
            }
        },
        calendar : {
            sameDay : '[今天] LT',
            nextDay : '[明天] LT',
            nextWeek : '[下]dddd LT',
            lastDay : '[昨天] LT',
            lastWeek : '[上]dddd LT',
            sameElse : 'L'
        },
        dayOfMonthOrdinalParse: /\d{1,2}(日|月|週)/,
        ordinal : function (number, period) {
            switch (period) {
                case 'd' :
                case 'D' :
                case 'DDD' :
                    return number + '日';
                case 'M' :
                    return number + '月';
                case 'w' :
                case 'W' :
                    return number + '週';
                default :
                    return number;
            }
        },
        relativeTime : {
            future : '%s內',
            past : '%s前',
            s : '幾秒',
            ss : '%d 秒',
            m : '1 分鐘',
            mm : '%d 分鐘',
            h : '1 小時',
            hh : '%d 小時',
            d : '1 天',
            dd : '%d 天',
            M : '1 個月',
            MM : '%d 個月',
            y : '1 年',
            yy : '%d 年'
        }
    });

    hooks.locale('en');

    return hooks;

})));

var collect =
/******/ (function(modules) { // webpackBootstrap
/******/ 	// The module cache
/******/ 	var installedModules = {};
/******/
/******/ 	// The require function
/******/ 	function __webpack_require__(moduleId) {
/******/
/******/ 		// Check if module is in cache
/******/ 		if(installedModules[moduleId]) {
/******/ 			return installedModules[moduleId].exports;
/******/ 		}
/******/ 		// Create a new module (and put it into the cache)
/******/ 		var module = installedModules[moduleId] = {
/******/ 			i: moduleId,
/******/ 			l: false,
/******/ 			exports: {}
/******/ 		};
/******/
/******/ 		// Execute the module function
/******/ 		modules[moduleId].call(module.exports, module, module.exports, __webpack_require__);
/******/
/******/ 		// Flag the module as loaded
/******/ 		module.l = true;
/******/
/******/ 		// Return the exports of the module
/******/ 		return module.exports;
/******/ 	}
/******/
/******/
/******/ 	// expose the modules object (__webpack_modules__)
/******/ 	__webpack_require__.m = modules;
/******/
/******/ 	// expose the module cache
/******/ 	__webpack_require__.c = installedModules;
/******/
/******/ 	// define getter function for harmony exports
/******/ 	__webpack_require__.d = function(exports, name, getter) {
/******/ 		if(!__webpack_require__.o(exports, name)) {
/******/ 			Object.defineProperty(exports, name, { enumerable: true, get: getter });
/******/ 		}
/******/ 	};
/******/
/******/ 	// define __esModule on exports
/******/ 	__webpack_require__.r = function(exports) {
/******/ 		if(typeof Symbol !== 'undefined' && Symbol.toStringTag) {
/******/ 			Object.defineProperty(exports, Symbol.toStringTag, { value: 'Module' });
/******/ 		}
/******/ 		Object.defineProperty(exports, '__esModule', { value: true });
/******/ 	};
/******/
/******/ 	// create a fake namespace object
/******/ 	// mode & 1: value is a module id, require it
/******/ 	// mode & 2: merge all properties of value into the ns
/******/ 	// mode & 4: return value when already ns object
/******/ 	// mode & 8|1: behave like require
/******/ 	__webpack_require__.t = function(value, mode) {
/******/ 		if(mode & 1) value = __webpack_require__(value);
/******/ 		if(mode & 8) return value;
/******/ 		if((mode & 4) && typeof value === 'object' && value && value.__esModule) return value;
/******/ 		var ns = Object.create(null);
/******/ 		__webpack_require__.r(ns);
/******/ 		Object.defineProperty(ns, 'default', { enumerable: true, value: value });
/******/ 		if(mode & 2 && typeof value != 'string') for(var key in value) __webpack_require__.d(ns, key, function(key) { return value[key]; }.bind(null, key));
/******/ 		return ns;
/******/ 	};
/******/
/******/ 	// getDefaultExport function for compatibility with non-harmony modules
/******/ 	__webpack_require__.n = function(module) {
/******/ 		var getter = module && module.__esModule ?
/******/ 			function getDefault() { return module['default']; } :
/******/ 			function getModuleExports() { return module; };
/******/ 		__webpack_require__.d(getter, 'a', getter);
/******/ 		return getter;
/******/ 	};
/******/
/******/ 	// Object.prototype.hasOwnProperty.call
/******/ 	__webpack_require__.o = function(object, property) { return Object.prototype.hasOwnProperty.call(object, property); };
/******/
/******/ 	// __webpack_public_path__
/******/ 	__webpack_require__.p = "";
/******/
/******/
/******/ 	// Load entry module and return exports
/******/ 	return __webpack_require__(__webpack_require__.s = "./dist/index.js");
/******/ })
/************************************************************************/
/******/ ({

/***/ "./dist/helpers/clone.js":
/*!*******************************!*\
  !*** ./dist/helpers/clone.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\n/**\n * Clone helper\n *\n * Clone an array or object\n *\n * @param items\n * @returns {*}\n */\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function clone(items) {\n  var cloned = void 0;\n\n  if (Array.isArray(items)) {\n    var _cloned;\n\n    cloned = [];\n\n    (_cloned = cloned).push.apply(_cloned, _toConsumableArray(items));\n  } else {\n    cloned = {};\n\n    Object.keys(items).forEach(function (prop) {\n      cloned[prop] = items[prop];\n    });\n  }\n\n  return cloned;\n};\n\n//# sourceURL=webpack://collect/./dist/helpers/clone.js?");

/***/ }),

/***/ "./dist/helpers/is.js":
/*!****************************!*\
  !*** ./dist/helpers/is.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = {\n  /**\n   * @returns {boolean}\n   */\n  isArray: function isArray(item) {\n    return Array.isArray(item);\n  },\n\n  /**\n   * @returns {boolean}\n   */\n  isObject: function isObject(item) {\n    return (typeof item === 'undefined' ? 'undefined' : _typeof(item)) === 'object' && item !== null;\n  },\n\n  /**\n   * @returns {boolean}\n   */\n  isFunction: function isFunction(item) {\n    return typeof item === 'function';\n  }\n};\n\n//# sourceURL=webpack://collect/./dist/helpers/is.js?");

/***/ }),

/***/ "./dist/helpers/nestedValue.js":
/*!*************************************!*\
  !*** ./dist/helpers/nestedValue.js ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\n/**\n * Get value of a nested property\n *\n * @param mainObject\n * @param key\n * @returns {*}\n */\n\nmodule.exports = function nestedValue(mainObject, key) {\n  try {\n    return key.split('.').reduce(function (obj, property) {\n      return obj[property];\n    }, mainObject);\n  } catch (err) {\n    return null;\n  }\n};\n\n//# sourceURL=webpack://collect/./dist/helpers/nestedValue.js?");

/***/ }),

/***/ "./dist/helpers/values.js":
/*!********************************!*\
  !*** ./dist/helpers/values.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\n\n/**\n * Values helper\n *\n * Retrieve values from [this.items] when it is an array, object or Collection\n *\n * @returns {*}\n * @param items\n */\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function values(items) {\n  var valuesArray = [];\n\n  if (Array.isArray(items)) {\n    valuesArray.push.apply(valuesArray, _toConsumableArray(items));\n  } else if (items.constructor.name === 'Collection') {\n    valuesArray.push.apply(valuesArray, _toConsumableArray(items.all()));\n  } else {\n    Object.keys(items).forEach(function (prop) {\n      return valuesArray.push(items[prop]);\n    });\n  }\n\n  return valuesArray;\n};\n\n//# sourceURL=webpack://collect/./dist/helpers/values.js?");

/***/ }),

/***/ "./dist/helpers/variadic.js":
/*!**********************************!*\
  !*** ./dist/helpers/variadic.js ***!
  \**********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\n/**\n * Variadic helper function\n *\n * @param args\n * @returns {*}\n */\n\nmodule.exports = function variadic(args) {\n  if (Array.isArray(args[0])) {\n    return args[0];\n  }\n\n  return args;\n};\n\n//# sourceURL=webpack://collect/./dist/helpers/variadic.js?");

/***/ }),

/***/ "./dist/index.js":
/*!***********************!*\
  !*** ./dist/index.js ***!
  \***********************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nfunction Collection(collection) {\n  if (collection !== undefined && !Array.isArray(collection) && (typeof collection === 'undefined' ? 'undefined' : _typeof(collection)) !== 'object') {\n    this.items = [collection];\n  } else if (collection instanceof this.constructor) {\n    this.items = collection.all();\n  } else {\n    this.items = collection || [];\n  }\n}\n\n/**\n * Symbol.iterator\n * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Symbol/iterator\n */\nvar SymbolIterator = __webpack_require__(/*! ./methods/symbol.iterator */ \"./dist/methods/symbol.iterator.js\");\n\nif (typeof Symbol !== 'undefined') {\n  Collection.prototype[Symbol.iterator] = SymbolIterator;\n}\n\n/**\n * Support JSON.stringify\n * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/stringify\n */\nCollection.prototype.toJSON = function toJSON() {\n  return this.items;\n};\n\nCollection.prototype.all = __webpack_require__(/*! ./methods/all */ \"./dist/methods/all.js\");\nCollection.prototype.average = __webpack_require__(/*! ./methods/average */ \"./dist/methods/average.js\");\nCollection.prototype.avg = __webpack_require__(/*! ./methods/average */ \"./dist/methods/average.js\");\nCollection.prototype.chunk = __webpack_require__(/*! ./methods/chunk */ \"./dist/methods/chunk.js\");\nCollection.prototype.collapse = __webpack_require__(/*! ./methods/collapse */ \"./dist/methods/collapse.js\");\nCollection.prototype.combine = __webpack_require__(/*! ./methods/combine */ \"./dist/methods/combine.js\");\nCollection.prototype.concat = __webpack_require__(/*! ./methods/concat */ \"./dist/methods/concat.js\");\nCollection.prototype.contains = __webpack_require__(/*! ./methods/contains */ \"./dist/methods/contains.js\");\nCollection.prototype.count = __webpack_require__(/*! ./methods/count */ \"./dist/methods/count.js\");\nCollection.prototype.countBy = __webpack_require__(/*! ./methods/countBy */ \"./dist/methods/countBy.js\");\nCollection.prototype.crossJoin = __webpack_require__(/*! ./methods/crossJoin */ \"./dist/methods/crossJoin.js\");\nCollection.prototype.dd = __webpack_require__(/*! ./methods/dd */ \"./dist/methods/dd.js\");\nCollection.prototype.diff = __webpack_require__(/*! ./methods/diff */ \"./dist/methods/diff.js\");\nCollection.prototype.diffAssoc = __webpack_require__(/*! ./methods/diffAssoc */ \"./dist/methods/diffAssoc.js\");\nCollection.prototype.diffKeys = __webpack_require__(/*! ./methods/diffKeys */ \"./dist/methods/diffKeys.js\");\nCollection.prototype.dump = __webpack_require__(/*! ./methods/dump */ \"./dist/methods/dump.js\");\nCollection.prototype.duplicates = __webpack_require__(/*! ./methods/duplicates */ \"./dist/methods/duplicates.js\");\nCollection.prototype.each = __webpack_require__(/*! ./methods/each */ \"./dist/methods/each.js\");\nCollection.prototype.eachSpread = __webpack_require__(/*! ./methods/eachSpread */ \"./dist/methods/eachSpread.js\");\nCollection.prototype.every = __webpack_require__(/*! ./methods/every */ \"./dist/methods/every.js\");\nCollection.prototype.except = __webpack_require__(/*! ./methods/except */ \"./dist/methods/except.js\");\nCollection.prototype.filter = __webpack_require__(/*! ./methods/filter */ \"./dist/methods/filter.js\");\nCollection.prototype.first = __webpack_require__(/*! ./methods/first */ \"./dist/methods/first.js\");\nCollection.prototype.firstWhere = __webpack_require__(/*! ./methods/firstWhere */ \"./dist/methods/firstWhere.js\");\nCollection.prototype.flatMap = __webpack_require__(/*! ./methods/flatMap */ \"./dist/methods/flatMap.js\");\nCollection.prototype.flatten = __webpack_require__(/*! ./methods/flatten */ \"./dist/methods/flatten.js\");\nCollection.prototype.flip = __webpack_require__(/*! ./methods/flip */ \"./dist/methods/flip.js\");\nCollection.prototype.forPage = __webpack_require__(/*! ./methods/forPage */ \"./dist/methods/forPage.js\");\nCollection.prototype.forget = __webpack_require__(/*! ./methods/forget */ \"./dist/methods/forget.js\");\nCollection.prototype.get = __webpack_require__(/*! ./methods/get */ \"./dist/methods/get.js\");\nCollection.prototype.groupBy = __webpack_require__(/*! ./methods/groupBy */ \"./dist/methods/groupBy.js\");\nCollection.prototype.has = __webpack_require__(/*! ./methods/has */ \"./dist/methods/has.js\");\nCollection.prototype.implode = __webpack_require__(/*! ./methods/implode */ \"./dist/methods/implode.js\");\nCollection.prototype.intersect = __webpack_require__(/*! ./methods/intersect */ \"./dist/methods/intersect.js\");\nCollection.prototype.intersectByKeys = __webpack_require__(/*! ./methods/intersectByKeys */ \"./dist/methods/intersectByKeys.js\");\nCollection.prototype.isEmpty = __webpack_require__(/*! ./methods/isEmpty */ \"./dist/methods/isEmpty.js\");\nCollection.prototype.isNotEmpty = __webpack_require__(/*! ./methods/isNotEmpty */ \"./dist/methods/isNotEmpty.js\");\nCollection.prototype.join = __webpack_require__(/*! ./methods/join */ \"./dist/methods/join.js\");\nCollection.prototype.keyBy = __webpack_require__(/*! ./methods/keyBy */ \"./dist/methods/keyBy.js\");\nCollection.prototype.keys = __webpack_require__(/*! ./methods/keys */ \"./dist/methods/keys.js\");\nCollection.prototype.last = __webpack_require__(/*! ./methods/last */ \"./dist/methods/last.js\");\nCollection.prototype.macro = __webpack_require__(/*! ./methods/macro */ \"./dist/methods/macro.js\");\nCollection.prototype.make = __webpack_require__(/*! ./methods/make */ \"./dist/methods/make.js\");\nCollection.prototype.map = __webpack_require__(/*! ./methods/map */ \"./dist/methods/map.js\");\nCollection.prototype.mapSpread = __webpack_require__(/*! ./methods/mapSpread */ \"./dist/methods/mapSpread.js\");\nCollection.prototype.mapToDictionary = __webpack_require__(/*! ./methods/mapToDictionary */ \"./dist/methods/mapToDictionary.js\");\nCollection.prototype.mapInto = __webpack_require__(/*! ./methods/mapInto */ \"./dist/methods/mapInto.js\");\nCollection.prototype.mapToGroups = __webpack_require__(/*! ./methods/mapToGroups */ \"./dist/methods/mapToGroups.js\");\nCollection.prototype.mapWithKeys = __webpack_require__(/*! ./methods/mapWithKeys */ \"./dist/methods/mapWithKeys.js\");\nCollection.prototype.max = __webpack_require__(/*! ./methods/max */ \"./dist/methods/max.js\");\nCollection.prototype.median = __webpack_require__(/*! ./methods/median */ \"./dist/methods/median.js\");\nCollection.prototype.merge = __webpack_require__(/*! ./methods/merge */ \"./dist/methods/merge.js\");\nCollection.prototype.mergeRecursive = __webpack_require__(/*! ./methods/mergeRecursive */ \"./dist/methods/mergeRecursive.js\");\nCollection.prototype.min = __webpack_require__(/*! ./methods/min */ \"./dist/methods/min.js\");\nCollection.prototype.mode = __webpack_require__(/*! ./methods/mode */ \"./dist/methods/mode.js\");\nCollection.prototype.nth = __webpack_require__(/*! ./methods/nth */ \"./dist/methods/nth.js\");\nCollection.prototype.only = __webpack_require__(/*! ./methods/only */ \"./dist/methods/only.js\");\nCollection.prototype.pad = __webpack_require__(/*! ./methods/pad */ \"./dist/methods/pad.js\");\nCollection.prototype.partition = __webpack_require__(/*! ./methods/partition */ \"./dist/methods/partition.js\");\nCollection.prototype.pipe = __webpack_require__(/*! ./methods/pipe */ \"./dist/methods/pipe.js\");\nCollection.prototype.pluck = __webpack_require__(/*! ./methods/pluck */ \"./dist/methods/pluck.js\");\nCollection.prototype.pop = __webpack_require__(/*! ./methods/pop */ \"./dist/methods/pop.js\");\nCollection.prototype.prepend = __webpack_require__(/*! ./methods/prepend */ \"./dist/methods/prepend.js\");\nCollection.prototype.pull = __webpack_require__(/*! ./methods/pull */ \"./dist/methods/pull.js\");\nCollection.prototype.push = __webpack_require__(/*! ./methods/push */ \"./dist/methods/push.js\");\nCollection.prototype.put = __webpack_require__(/*! ./methods/put */ \"./dist/methods/put.js\");\nCollection.prototype.random = __webpack_require__(/*! ./methods/random */ \"./dist/methods/random.js\");\nCollection.prototype.reduce = __webpack_require__(/*! ./methods/reduce */ \"./dist/methods/reduce.js\");\nCollection.prototype.reject = __webpack_require__(/*! ./methods/reject */ \"./dist/methods/reject.js\");\nCollection.prototype.replace = __webpack_require__(/*! ./methods/replace */ \"./dist/methods/replace.js\");\nCollection.prototype.replaceRecursive = __webpack_require__(/*! ./methods/replaceRecursive */ \"./dist/methods/replaceRecursive.js\");\nCollection.prototype.reverse = __webpack_require__(/*! ./methods/reverse */ \"./dist/methods/reverse.js\");\nCollection.prototype.search = __webpack_require__(/*! ./methods/search */ \"./dist/methods/search.js\");\nCollection.prototype.shift = __webpack_require__(/*! ./methods/shift */ \"./dist/methods/shift.js\");\nCollection.prototype.shuffle = __webpack_require__(/*! ./methods/shuffle */ \"./dist/methods/shuffle.js\");\nCollection.prototype.slice = __webpack_require__(/*! ./methods/slice */ \"./dist/methods/slice.js\");\nCollection.prototype.some = __webpack_require__(/*! ./methods/contains */ \"./dist/methods/contains.js\");\nCollection.prototype.sort = __webpack_require__(/*! ./methods/sort */ \"./dist/methods/sort.js\");\nCollection.prototype.sortBy = __webpack_require__(/*! ./methods/sortBy */ \"./dist/methods/sortBy.js\");\nCollection.prototype.sortByDesc = __webpack_require__(/*! ./methods/sortByDesc */ \"./dist/methods/sortByDesc.js\");\nCollection.prototype.sortKeys = __webpack_require__(/*! ./methods/sortKeys */ \"./dist/methods/sortKeys.js\");\nCollection.prototype.sortKeysDesc = __webpack_require__(/*! ./methods/sortKeysDesc */ \"./dist/methods/sortKeysDesc.js\");\nCollection.prototype.splice = __webpack_require__(/*! ./methods/splice */ \"./dist/methods/splice.js\");\nCollection.prototype.split = __webpack_require__(/*! ./methods/split */ \"./dist/methods/split.js\");\nCollection.prototype.sum = __webpack_require__(/*! ./methods/sum */ \"./dist/methods/sum.js\");\nCollection.prototype.take = __webpack_require__(/*! ./methods/take */ \"./dist/methods/take.js\");\nCollection.prototype.tap = __webpack_require__(/*! ./methods/tap */ \"./dist/methods/tap.js\");\nCollection.prototype.times = __webpack_require__(/*! ./methods/times */ \"./dist/methods/times.js\");\nCollection.prototype.toArray = __webpack_require__(/*! ./methods/toArray */ \"./dist/methods/toArray.js\");\nCollection.prototype.toJson = __webpack_require__(/*! ./methods/toJson */ \"./dist/methods/toJson.js\");\nCollection.prototype.transform = __webpack_require__(/*! ./methods/transform */ \"./dist/methods/transform.js\");\nCollection.prototype.unless = __webpack_require__(/*! ./methods/unless */ \"./dist/methods/unless.js\");\nCollection.prototype.unlessEmpty = __webpack_require__(/*! ./methods/whenNotEmpty */ \"./dist/methods/whenNotEmpty.js\");\nCollection.prototype.unlessNotEmpty = __webpack_require__(/*! ./methods/whenEmpty */ \"./dist/methods/whenEmpty.js\");\nCollection.prototype.union = __webpack_require__(/*! ./methods/union */ \"./dist/methods/union.js\");\nCollection.prototype.unique = __webpack_require__(/*! ./methods/unique */ \"./dist/methods/unique.js\");\nCollection.prototype.unwrap = __webpack_require__(/*! ./methods/unwrap */ \"./dist/methods/unwrap.js\");\nCollection.prototype.values = __webpack_require__(/*! ./methods/values */ \"./dist/methods/values.js\");\nCollection.prototype.when = __webpack_require__(/*! ./methods/when */ \"./dist/methods/when.js\");\nCollection.prototype.whenEmpty = __webpack_require__(/*! ./methods/whenEmpty */ \"./dist/methods/whenEmpty.js\");\nCollection.prototype.whenNotEmpty = __webpack_require__(/*! ./methods/whenNotEmpty */ \"./dist/methods/whenNotEmpty.js\");\nCollection.prototype.where = __webpack_require__(/*! ./methods/where */ \"./dist/methods/where.js\");\nCollection.prototype.whereBetween = __webpack_require__(/*! ./methods/whereBetween */ \"./dist/methods/whereBetween.js\");\nCollection.prototype.whereIn = __webpack_require__(/*! ./methods/whereIn */ \"./dist/methods/whereIn.js\");\nCollection.prototype.whereInstanceOf = __webpack_require__(/*! ./methods/whereInstanceOf */ \"./dist/methods/whereInstanceOf.js\");\nCollection.prototype.whereNotBetween = __webpack_require__(/*! ./methods/whereNotBetween */ \"./dist/methods/whereNotBetween.js\");\nCollection.prototype.whereNotIn = __webpack_require__(/*! ./methods/whereNotIn */ \"./dist/methods/whereNotIn.js\");\nCollection.prototype.wrap = __webpack_require__(/*! ./methods/wrap */ \"./dist/methods/wrap.js\");\nCollection.prototype.zip = __webpack_require__(/*! ./methods/zip */ \"./dist/methods/zip.js\");\n\nvar collect = function collect(collection) {\n  return new Collection(collection);\n};\n\nmodule.exports = collect;\nmodule.exports.collect = collect;\nmodule.exports.default = collect;\n\n//# sourceURL=webpack://collect/./dist/index.js?");

/***/ }),

/***/ "./dist/methods/all.js":
/*!*****************************!*\
  !*** ./dist/methods/all.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function all() {\n  return this.items;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/all.js?");

/***/ }),

/***/ "./dist/methods/average.js":
/*!*********************************!*\
  !*** ./dist/methods/average.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function average(key) {\n  if (key === undefined) {\n    return this.sum() / this.items.length;\n  }\n\n  return new this.constructor(this.items).pluck(key).sum() / this.items.length;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/average.js?");

/***/ }),

/***/ "./dist/methods/chunk.js":
/*!*******************************!*\
  !*** ./dist/methods/chunk.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function chunk(size) {\n  var _this = this;\n\n  var chunks = [];\n  var index = 0;\n\n  if (Array.isArray(this.items)) {\n    do {\n      var items = this.items.slice(index, index + size);\n      var collection = new this.constructor(items);\n\n      chunks.push(collection);\n      index += size;\n    } while (index < this.items.length);\n  } else if (_typeof(this.items) === 'object') {\n    var keys = Object.keys(this.items);\n\n    var _loop = function _loop() {\n      var keysOfChunk = keys.slice(index, index + size);\n      var collection = new _this.constructor({});\n\n      keysOfChunk.forEach(function (key) {\n        return collection.put(key, _this.items[key]);\n      });\n\n      chunks.push(collection);\n      index += size;\n    };\n\n    do {\n      _loop();\n    } while (index < keys.length);\n  } else {\n    chunks.push(new this.constructor([this.items]));\n  }\n\n  return new this.constructor(chunks);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/chunk.js?");

/***/ }),

/***/ "./dist/methods/collapse.js":
/*!**********************************!*\
  !*** ./dist/methods/collapse.js ***!
  \**********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function collapse() {\n  var _ref;\n\n  return new this.constructor((_ref = []).concat.apply(_ref, _toConsumableArray(this.items)));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/collapse.js?");

/***/ }),

/***/ "./dist/methods/combine.js":
/*!*********************************!*\
  !*** ./dist/methods/combine.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i[\"return\"]) _i[\"return\"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError(\"Invalid attempt to destructure non-iterable instance\"); } }; }();\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function combine(array) {\n  var _this = this;\n\n  var values = array;\n\n  if (values instanceof this.constructor) {\n    values = array.all();\n  }\n\n  var collection = {};\n\n  if (Array.isArray(this.items) && Array.isArray(values)) {\n    this.items.forEach(function (key, iterator) {\n      collection[key] = values[iterator];\n    });\n  } else if (_typeof(this.items) === 'object' && (typeof values === 'undefined' ? 'undefined' : _typeof(values)) === 'object') {\n    Object.keys(this.items).forEach(function (key, index) {\n      collection[_this.items[key]] = values[Object.keys(values)[index]];\n    });\n  } else if (Array.isArray(this.items)) {\n    collection[this.items[0]] = values;\n  } else if (typeof this.items === 'string' && Array.isArray(values)) {\n    var _values = values;\n\n    var _values2 = _slicedToArray(_values, 1);\n\n    collection[this.items] = _values2[0];\n  } else if (typeof this.items === 'string') {\n    collection[this.items] = values;\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/combine.js?");

/***/ }),

/***/ "./dist/methods/concat.js":
/*!********************************!*\
  !*** ./dist/methods/concat.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nvar clone = __webpack_require__(/*! ../helpers/clone */ \"./dist/helpers/clone.js\");\n\nmodule.exports = function concat(collectionOrArrayOrObject) {\n  var list = collectionOrArrayOrObject;\n\n  if (collectionOrArrayOrObject instanceof this.constructor) {\n    list = collectionOrArrayOrObject.all();\n  } else if ((typeof collectionOrArrayOrObject === 'undefined' ? 'undefined' : _typeof(collectionOrArrayOrObject)) === 'object') {\n    list = [];\n    Object.keys(collectionOrArrayOrObject).forEach(function (property) {\n      list.push(collectionOrArrayOrObject[property]);\n    });\n  }\n\n  var collection = clone(this.items);\n\n  list.forEach(function (item) {\n    if ((typeof item === 'undefined' ? 'undefined' : _typeof(item)) === 'object') {\n      Object.keys(item).forEach(function (key) {\n        return collection.push(item[key]);\n      });\n    } else {\n      collection.push(item);\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/concat.js?");

/***/ }),

/***/ "./dist/methods/contains.js":
/*!**********************************!*\
  !*** ./dist/methods/contains.js ***!
  \**********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function contains(key, value) {\n  if (value !== undefined) {\n    if (Array.isArray(this.items)) {\n      return this.items.filter(function (items) {\n        return items[key] !== undefined && items[key] === value;\n      }).length > 0;\n    }\n\n    return this.items[key] !== undefined && this.items[key] === value;\n  }\n\n  if (isFunction(key)) {\n    return this.items.filter(function (item, index) {\n      return key(item, index);\n    }).length > 0;\n  }\n\n  if (Array.isArray(this.items)) {\n    return this.items.indexOf(key) !== -1;\n  }\n\n  var keysAndValues = values(this.items);\n  keysAndValues.push.apply(keysAndValues, _toConsumableArray(Object.keys(this.items)));\n\n  return keysAndValues.indexOf(key) !== -1;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/contains.js?");

/***/ }),

/***/ "./dist/methods/count.js":
/*!*******************************!*\
  !*** ./dist/methods/count.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function count() {\n  var arrayLength = 0;\n\n  if (Array.isArray(this.items)) {\n    arrayLength = this.items.length;\n  }\n\n  return Math.max(Object.keys(this.items).length, arrayLength);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/count.js?");

/***/ }),

/***/ "./dist/methods/countBy.js":
/*!*********************************!*\
  !*** ./dist/methods/countBy.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function countBy() {\n  var fn = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : function (value) {\n    return value;\n  };\n\n  return new this.constructor(this.items).groupBy(fn).map(function (value) {\n    return value.count();\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/countBy.js?");

/***/ }),

/***/ "./dist/methods/crossJoin.js":
/*!***********************************!*\
  !*** ./dist/methods/crossJoin.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function crossJoin() {\n  function join(collection, constructor, args) {\n    var current = args[0];\n\n    if (current instanceof constructor) {\n      current = current.all();\n    }\n\n    var rest = args.slice(1);\n    var last = !rest.length;\n    var result = [];\n\n    for (var i = 0; i < current.length; i += 1) {\n      var collectionCopy = collection.slice();\n      collectionCopy.push(current[i]);\n\n      if (last) {\n        result.push(collectionCopy);\n      } else {\n        result = result.concat(join(collectionCopy, constructor, rest));\n      }\n    }\n\n    return result;\n  }\n\n  for (var _len = arguments.length, values = Array(_len), _key = 0; _key < _len; _key++) {\n    values[_key] = arguments[_key];\n  }\n\n  return new this.constructor(join([], this.constructor, [].concat([this.items], values)));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/crossJoin.js?");

/***/ }),

/***/ "./dist/methods/dd.js":
/*!****************************!*\
  !*** ./dist/methods/dd.js ***!
  \****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("/* WEBPACK VAR INJECTION */(function(process) {\n\nmodule.exports = function dd() {\n  this.dump();\n\n  if (typeof process !== 'undefined') {\n    process.exit(1);\n  }\n};\n/* WEBPACK VAR INJECTION */}.call(this, __webpack_require__(/*! ./../../node_modules/process/browser.js */ \"./node_modules/process/browser.js\")))\n\n//# sourceURL=webpack://collect/./dist/methods/dd.js?");

/***/ }),

/***/ "./dist/methods/diff.js":
/*!******************************!*\
  !*** ./dist/methods/diff.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function diff(values) {\n  var valuesToDiff = void 0;\n\n  if (values instanceof this.constructor) {\n    valuesToDiff = values.all();\n  } else {\n    valuesToDiff = values;\n  }\n\n  var collection = this.items.filter(function (item) {\n    return valuesToDiff.indexOf(item) === -1;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/diff.js?");

/***/ }),

/***/ "./dist/methods/diffAssoc.js":
/*!***********************************!*\
  !*** ./dist/methods/diffAssoc.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function diffAssoc(values) {\n  var _this = this;\n\n  var diffValues = values;\n\n  if (values instanceof this.constructor) {\n    diffValues = values.all();\n  }\n\n  var collection = {};\n\n  Object.keys(this.items).forEach(function (key) {\n    if (diffValues[key] === undefined || diffValues[key] !== _this.items[key]) {\n      collection[key] = _this.items[key];\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/diffAssoc.js?");

/***/ }),

/***/ "./dist/methods/diffKeys.js":
/*!**********************************!*\
  !*** ./dist/methods/diffKeys.js ***!
  \**********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function diffKeys(object) {\n  var objectToDiff = void 0;\n\n  if (object instanceof this.constructor) {\n    objectToDiff = object.all();\n  } else {\n    objectToDiff = object;\n  }\n\n  var objectKeys = Object.keys(objectToDiff);\n\n  var remainingKeys = Object.keys(this.items).filter(function (item) {\n    return objectKeys.indexOf(item) === -1;\n  });\n\n  return new this.constructor(this.items).only(remainingKeys);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/diffKeys.js?");

/***/ }),

/***/ "./dist/methods/dump.js":
/*!******************************!*\
  !*** ./dist/methods/dump.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function dump() {\n  // eslint-disable-next-line\n  console.log(this);\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/dump.js?");

/***/ }),

/***/ "./dist/methods/duplicates.js":
/*!************************************!*\
  !*** ./dist/methods/duplicates.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function duplicates() {\n  var _this = this;\n\n  var occuredValues = [];\n  var duplicateValues = {};\n\n  var stringifiedValue = function stringifiedValue(value) {\n    if (Array.isArray(value) || (typeof value === 'undefined' ? 'undefined' : _typeof(value)) === 'object') {\n      return JSON.stringify(value);\n    }\n\n    return value;\n  };\n\n  if (Array.isArray(this.items)) {\n    this.items.forEach(function (value, index) {\n      var valueAsString = stringifiedValue(value);\n\n      if (occuredValues.indexOf(valueAsString) === -1) {\n        occuredValues.push(valueAsString);\n      } else {\n        duplicateValues[index] = value;\n      }\n    });\n  } else if (_typeof(this.items) === 'object') {\n    Object.keys(this.items).forEach(function (key) {\n      var valueAsString = stringifiedValue(_this.items[key]);\n\n      if (occuredValues.indexOf(valueAsString) === -1) {\n        occuredValues.push(valueAsString);\n      } else {\n        duplicateValues[key] = _this.items[key];\n      }\n    });\n  }\n\n  return new this.constructor(duplicateValues);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/duplicates.js?");

/***/ }),

/***/ "./dist/methods/each.js":
/*!******************************!*\
  !*** ./dist/methods/each.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function each(fn) {\n  var stop = false;\n\n  if (Array.isArray(this.items)) {\n    var length = this.items.length;\n\n\n    for (var index = 0; index < length && !stop; index += 1) {\n      stop = fn(this.items[index], index, this.items) === false;\n    }\n  } else {\n    var keys = Object.keys(this.items);\n    var _length = keys.length;\n\n\n    for (var _index = 0; _index < _length && !stop; _index += 1) {\n      var key = keys[_index];\n\n      stop = fn(this.items[key], key, this.items) === false;\n    }\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/each.js?");

/***/ }),

/***/ "./dist/methods/eachSpread.js":
/*!************************************!*\
  !*** ./dist/methods/eachSpread.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function eachSpread(fn) {\n  this.each(function (values, key) {\n    fn.apply(undefined, _toConsumableArray(values).concat([key]));\n  });\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/eachSpread.js?");

/***/ }),

/***/ "./dist/methods/every.js":
/*!*******************************!*\
  !*** ./dist/methods/every.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nmodule.exports = function every(fn) {\n  var items = values(this.items);\n\n  return items.map(function (item, index) {\n    return fn(item, index);\n  }).indexOf(false) === -1;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/every.js?");

/***/ }),

/***/ "./dist/methods/except.js":
/*!********************************!*\
  !*** ./dist/methods/except.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar variadic = __webpack_require__(/*! ../helpers/variadic */ \"./dist/helpers/variadic.js\");\n\nmodule.exports = function except() {\n  var _this = this;\n\n  for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {\n    args[_key] = arguments[_key];\n  }\n\n  var properties = variadic(args);\n\n  if (Array.isArray(this.items)) {\n    var _collection = this.items.filter(function (item) {\n      return properties.indexOf(item) === -1;\n    });\n\n    return new this.constructor(_collection);\n  }\n\n  var collection = {};\n\n  Object.keys(this.items).forEach(function (property) {\n    if (properties.indexOf(property) === -1) {\n      collection[property] = _this.items[property];\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/except.js?");

/***/ }),

/***/ "./dist/methods/filter.js":
/*!********************************!*\
  !*** ./dist/methods/filter.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nfunction falsyValue(item) {\n  if (Array.isArray(item)) {\n    if (item.length) {\n      return false;\n    }\n  } else if (item !== undefined && item !== null && (typeof item === 'undefined' ? 'undefined' : _typeof(item)) === 'object') {\n    if (Object.keys(item).length) {\n      return false;\n    }\n  } else if (item) {\n    return false;\n  }\n\n  return true;\n}\n\nfunction filterObject(func, items) {\n  var result = {};\n  Object.keys(items).forEach(function (key) {\n    if (func) {\n      if (func(items[key], key)) {\n        result[key] = items[key];\n      }\n    } else if (!falsyValue(items[key])) {\n      result[key] = items[key];\n    }\n  });\n\n  return result;\n}\n\nfunction filterArray(func, items) {\n  if (func) {\n    return items.filter(func);\n  }\n  var result = [];\n  for (var i = 0; i < items.length; i += 1) {\n    var item = items[i];\n    if (!falsyValue(item)) {\n      result.push(item);\n    }\n  }\n\n  return result;\n}\n\nmodule.exports = function filter(fn) {\n  var func = fn || false;\n  var filteredItems = null;\n  if (Array.isArray(this.items)) {\n    filteredItems = filterArray(func, this.items);\n  } else {\n    filteredItems = filterObject(func, this.items);\n  }\n\n  return new this.constructor(filteredItems);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/filter.js?");

/***/ }),

/***/ "./dist/methods/first.js":
/*!*******************************!*\
  !*** ./dist/methods/first.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function first(fn, defaultValue) {\n  if (isFunction(fn)) {\n    for (var i = 0, length = this.items.length; i < length; i += 1) {\n      var item = this.items[i];\n      if (fn(item)) {\n        return item;\n      }\n    }\n\n    if (isFunction(defaultValue)) {\n      return defaultValue();\n    }\n\n    return defaultValue;\n  }\n\n  if (Array.isArray(this.items) && this.items.length || Object.keys(this.items).length) {\n    if (Array.isArray(this.items)) {\n      return this.items[0];\n    }\n\n    var firstKey = Object.keys(this.items)[0];\n\n    return this.items[firstKey];\n  }\n\n  if (isFunction(defaultValue)) {\n    return defaultValue();\n  }\n\n  return defaultValue;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/first.js?");

/***/ }),

/***/ "./dist/methods/firstWhere.js":
/*!************************************!*\
  !*** ./dist/methods/firstWhere.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function firstWhere(key, operator, value) {\n  return this.where(key, operator, value).first() || null;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/firstWhere.js?");

/***/ }),

/***/ "./dist/methods/flatMap.js":
/*!*********************************!*\
  !*** ./dist/methods/flatMap.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function flatMap(fn) {\n  return this.map(fn).collapse();\n};\n\n//# sourceURL=webpack://collect/./dist/methods/flatMap.js?");

/***/ }),

/***/ "./dist/methods/flatten.js":
/*!*********************************!*\
  !*** ./dist/methods/flatten.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isArray = _require.isArray,\n    isObject = _require.isObject;\n\nmodule.exports = function flatten(depth) {\n  var flattenDepth = depth || Infinity;\n\n  var fullyFlattened = false;\n  var collection = [];\n\n  var flat = function flat(items) {\n    collection = [];\n\n    if (isArray(items)) {\n      items.forEach(function (item) {\n        if (isArray(item)) {\n          collection = collection.concat(item);\n        } else if (isObject(item)) {\n          Object.keys(item).forEach(function (property) {\n            collection = collection.concat(item[property]);\n          });\n        } else {\n          collection.push(item);\n        }\n      });\n    } else {\n      Object.keys(items).forEach(function (property) {\n        if (isArray(items[property])) {\n          collection = collection.concat(items[property]);\n        } else if (isObject(items[property])) {\n          Object.keys(items).forEach(function (prop) {\n            collection = collection.concat(items[prop]);\n          });\n        } else {\n          collection.push(items[property]);\n        }\n      });\n    }\n\n    fullyFlattened = collection.filter(function (item) {\n      return isObject(item);\n    });\n    fullyFlattened = fullyFlattened.length === 0;\n\n    flattenDepth -= 1;\n  };\n\n  flat(this.items);\n\n  while (!fullyFlattened && flattenDepth > 0) {\n    flat(collection);\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/flatten.js?");

/***/ }),

/***/ "./dist/methods/flip.js":
/*!******************************!*\
  !*** ./dist/methods/flip.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function flip() {\n  var _this = this;\n\n  var collection = {};\n\n  if (Array.isArray(this.items)) {\n    Object.keys(this.items).forEach(function (key) {\n      collection[_this.items[key]] = Number(key);\n    });\n  } else {\n    Object.keys(this.items).forEach(function (key) {\n      collection[_this.items[key]] = key;\n    });\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/flip.js?");

/***/ }),

/***/ "./dist/methods/forPage.js":
/*!*********************************!*\
  !*** ./dist/methods/forPage.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function forPage(page, chunk) {\n  var _this = this;\n\n  var collection = {};\n\n  if (Array.isArray(this.items)) {\n    collection = this.items.slice(page * chunk - chunk, page * chunk);\n  } else {\n    Object.keys(this.items).slice(page * chunk - chunk, page * chunk).forEach(function (key) {\n      collection[key] = _this.items[key];\n    });\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/forPage.js?");

/***/ }),

/***/ "./dist/methods/forget.js":
/*!********************************!*\
  !*** ./dist/methods/forget.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function forget(key) {\n  if (Array.isArray(this.items)) {\n    this.items.splice(key, 1);\n  } else {\n    delete this.items[key];\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/forget.js?");

/***/ }),

/***/ "./dist/methods/get.js":
/*!*****************************!*\
  !*** ./dist/methods/get.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function get(key) {\n  var defaultValue = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : null;\n\n  if (this.items[key] !== undefined) {\n    return this.items[key];\n  }\n\n  if (isFunction(defaultValue)) {\n    return defaultValue();\n  }\n\n  if (defaultValue !== null) {\n    return defaultValue;\n  }\n\n  return null;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/get.js?");

/***/ }),

/***/ "./dist/methods/groupBy.js":
/*!*********************************!*\
  !*** ./dist/methods/groupBy.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function groupBy(key) {\n  var _this = this;\n\n  var collection = {};\n\n  this.items.forEach(function (item, index) {\n    var resolvedKey = void 0;\n\n    if (isFunction(key)) {\n      resolvedKey = key(item, index);\n    } else if (nestedValue(item, key) || nestedValue(item, key) === 0) {\n      resolvedKey = nestedValue(item, key);\n    } else {\n      resolvedKey = '';\n    }\n\n    if (collection[resolvedKey] === undefined) {\n      collection[resolvedKey] = new _this.constructor([]);\n    }\n\n    collection[resolvedKey].push(item);\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/groupBy.js?");

/***/ }),

/***/ "./dist/methods/has.js":
/*!*****************************!*\
  !*** ./dist/methods/has.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar variadic = __webpack_require__(/*! ../helpers/variadic */ \"./dist/helpers/variadic.js\");\n\nmodule.exports = function has() {\n  var _this = this;\n\n  for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {\n    args[_key] = arguments[_key];\n  }\n\n  var properties = variadic(args);\n\n  return properties.filter(function (key) {\n    return _this.items[key];\n  }).length === properties.length;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/has.js?");

/***/ }),

/***/ "./dist/methods/implode.js":
/*!*********************************!*\
  !*** ./dist/methods/implode.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function implode(key, glue) {\n  if (glue === undefined) {\n    return this.items.join(key);\n  }\n\n  return new this.constructor(this.items).pluck(key).all().join(glue);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/implode.js?");

/***/ }),

/***/ "./dist/methods/intersect.js":
/*!***********************************!*\
  !*** ./dist/methods/intersect.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function intersect(values) {\n  var intersectValues = values;\n\n  if (values instanceof this.constructor) {\n    intersectValues = values.all();\n  }\n\n  var collection = this.items.filter(function (item) {\n    return intersectValues.indexOf(item) !== -1;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/intersect.js?");

/***/ }),

/***/ "./dist/methods/intersectByKeys.js":
/*!*****************************************!*\
  !*** ./dist/methods/intersectByKeys.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function intersectByKeys(values) {\n  var _this = this;\n\n  var intersectKeys = Object.keys(values);\n\n  if (values instanceof this.constructor) {\n    intersectKeys = Object.keys(values.all());\n  }\n\n  var collection = {};\n\n  Object.keys(this.items).forEach(function (key) {\n    if (intersectKeys.indexOf(key) !== -1) {\n      collection[key] = _this.items[key];\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/intersectByKeys.js?");

/***/ }),

/***/ "./dist/methods/isEmpty.js":
/*!*********************************!*\
  !*** ./dist/methods/isEmpty.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function isEmpty() {\n  if (Array.isArray(this.items)) {\n    return !this.items.length;\n  }\n\n  return !Object.keys(this.items).length;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/isEmpty.js?");

/***/ }),

/***/ "./dist/methods/isNotEmpty.js":
/*!************************************!*\
  !*** ./dist/methods/isNotEmpty.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function isNotEmpty() {\n  return !this.isEmpty();\n};\n\n//# sourceURL=webpack://collect/./dist/methods/isNotEmpty.js?");

/***/ }),

/***/ "./dist/methods/join.js":
/*!******************************!*\
  !*** ./dist/methods/join.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function join(glue, finalGlue) {\n  var collection = this.values();\n\n  if (finalGlue === undefined) {\n    return collection.implode(glue);\n  }\n\n  var count = collection.count();\n\n  if (count === 0) {\n    return '';\n  }\n\n  if (count === 1) {\n    return collection.last();\n  }\n\n  var finalItem = collection.pop();\n\n  return collection.implode(glue) + finalGlue + finalItem;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/join.js?");

/***/ }),

/***/ "./dist/methods/keyBy.js":
/*!*******************************!*\
  !*** ./dist/methods/keyBy.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function keyBy(key) {\n  var collection = {};\n\n  if (isFunction(key)) {\n    this.items.forEach(function (item) {\n      collection[key(item)] = item;\n    });\n  } else {\n    this.items.forEach(function (item) {\n      var keyValue = nestedValue(item, key);\n\n      collection[keyValue || ''] = item;\n    });\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/keyBy.js?");

/***/ }),

/***/ "./dist/methods/keys.js":
/*!******************************!*\
  !*** ./dist/methods/keys.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function keys() {\n  var collection = Object.keys(this.items);\n\n  if (Array.isArray(this.items)) {\n    collection = collection.map(Number);\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/keys.js?");

/***/ }),

/***/ "./dist/methods/last.js":
/*!******************************!*\
  !*** ./dist/methods/last.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function last(fn, defaultValue) {\n  var items = this.items;\n\n\n  if (isFunction(fn)) {\n    items = this.filter(fn).all();\n  }\n\n  if (Array.isArray(items) && !items.length || !Object.keys(items).length) {\n    if (isFunction(defaultValue)) {\n      return defaultValue();\n    }\n\n    return defaultValue;\n  }\n\n  if (Array.isArray(items)) {\n    return items[items.length - 1];\n  }\n  var keys = Object.keys(items);\n\n  return items[keys[keys.length - 1]];\n};\n\n//# sourceURL=webpack://collect/./dist/methods/last.js?");

/***/ }),

/***/ "./dist/methods/macro.js":
/*!*******************************!*\
  !*** ./dist/methods/macro.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function macro(name, fn) {\n  this.constructor.prototype[name] = fn;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/macro.js?");

/***/ }),

/***/ "./dist/methods/make.js":
/*!******************************!*\
  !*** ./dist/methods/make.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function make() {\n  var items = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : [];\n\n  return new this.constructor(items);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/make.js?");

/***/ }),

/***/ "./dist/methods/map.js":
/*!*****************************!*\
  !*** ./dist/methods/map.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function map(fn) {\n  var _this = this;\n\n  if (Array.isArray(this.items)) {\n    return new this.constructor(this.items.map(fn));\n  }\n\n  var collection = {};\n\n  Object.keys(this.items).forEach(function (key) {\n    collection[key] = fn(_this.items[key], key);\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/map.js?");

/***/ }),

/***/ "./dist/methods/mapInto.js":
/*!*********************************!*\
  !*** ./dist/methods/mapInto.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function mapInto(ClassName) {\n  return this.map(function (value, key) {\n    return new ClassName(value, key);\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mapInto.js?");

/***/ }),

/***/ "./dist/methods/mapSpread.js":
/*!***********************************!*\
  !*** ./dist/methods/mapSpread.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function mapSpread(fn) {\n  return this.map(function (values, key) {\n    return fn.apply(undefined, _toConsumableArray(values).concat([key]));\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mapSpread.js?");

/***/ }),

/***/ "./dist/methods/mapToDictionary.js":
/*!*****************************************!*\
  !*** ./dist/methods/mapToDictionary.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i[\"return\"]) _i[\"return\"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError(\"Invalid attempt to destructure non-iterable instance\"); } }; }();\n\nmodule.exports = function mapToDictionary(fn) {\n  var collection = {};\n\n  this.items.forEach(function (item, k) {\n    var _fn = fn(item, k),\n        _fn2 = _slicedToArray(_fn, 2),\n        key = _fn2[0],\n        value = _fn2[1];\n\n    if (collection[key] === undefined) {\n      collection[key] = [value];\n    } else {\n      collection[key].push(value);\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mapToDictionary.js?");

/***/ }),

/***/ "./dist/methods/mapToGroups.js":
/*!*************************************!*\
  !*** ./dist/methods/mapToGroups.js ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i[\"return\"]) _i[\"return\"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError(\"Invalid attempt to destructure non-iterable instance\"); } }; }();\n\nmodule.exports = function mapToGroups(fn) {\n  var collection = {};\n\n  this.items.forEach(function (item, key) {\n    var _fn = fn(item, key),\n        _fn2 = _slicedToArray(_fn, 2),\n        keyed = _fn2[0],\n        value = _fn2[1];\n\n    if (collection[keyed] === undefined) {\n      collection[keyed] = [value];\n    } else {\n      collection[keyed].push(value);\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mapToGroups.js?");

/***/ }),

/***/ "./dist/methods/mapWithKeys.js":
/*!*************************************!*\
  !*** ./dist/methods/mapWithKeys.js ***!
  \*************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i[\"return\"]) _i[\"return\"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError(\"Invalid attempt to destructure non-iterable instance\"); } }; }();\n\nmodule.exports = function mapWithKeys(fn) {\n  var _this = this;\n\n  var collection = {};\n\n  if (Array.isArray(this.items)) {\n    this.items.forEach(function (item) {\n      var _fn = fn(item),\n          _fn2 = _slicedToArray(_fn, 2),\n          keyed = _fn2[0],\n          value = _fn2[1];\n\n      collection[keyed] = value;\n    });\n  } else {\n    Object.keys(this.items).forEach(function (key) {\n      var _fn3 = fn(_this.items[key]),\n          _fn4 = _slicedToArray(_fn3, 2),\n          keyed = _fn4[0],\n          value = _fn4[1];\n\n      collection[keyed] = value;\n    });\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mapWithKeys.js?");

/***/ }),

/***/ "./dist/methods/max.js":
/*!*****************************!*\
  !*** ./dist/methods/max.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function max(key) {\n  if (typeof key === 'string') {\n    var filtered = this.items.filter(function (item) {\n      return item[key] !== undefined;\n    });\n\n    return Math.max.apply(Math, _toConsumableArray(filtered.map(function (item) {\n      return item[key];\n    })));\n  }\n\n  return Math.max.apply(Math, _toConsumableArray(this.items));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/max.js?");

/***/ }),

/***/ "./dist/methods/median.js":
/*!********************************!*\
  !*** ./dist/methods/median.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function median(key) {\n  var length = this.items.length;\n\n\n  if (key === undefined) {\n    if (length % 2 === 0) {\n      return (this.items[length / 2 - 1] + this.items[length / 2]) / 2;\n    }\n\n    return this.items[Math.floor(length / 2)];\n  }\n\n  if (length % 2 === 0) {\n    return (this.items[length / 2 - 1][key] + this.items[length / 2][key]) / 2;\n  }\n\n  return this.items[Math.floor(length / 2)][key];\n};\n\n//# sourceURL=webpack://collect/./dist/methods/median.js?");

/***/ }),

/***/ "./dist/methods/merge.js":
/*!*******************************!*\
  !*** ./dist/methods/merge.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function merge(value) {\n  var arrayOrObject = value;\n\n  if (typeof arrayOrObject === 'string') {\n    arrayOrObject = [arrayOrObject];\n  }\n\n  if (Array.isArray(this.items) && Array.isArray(arrayOrObject)) {\n    return new this.constructor(this.items.concat(arrayOrObject));\n  }\n\n  var collection = JSON.parse(JSON.stringify(this.items));\n\n  Object.keys(arrayOrObject).forEach(function (key) {\n    collection[key] = arrayOrObject[key];\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/merge.js?");

/***/ }),

/***/ "./dist/methods/mergeRecursive.js":
/*!****************************************!*\
  !*** ./dist/methods/mergeRecursive.js ***!
  \****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function mergeRecursive(items) {\n  var merge = function merge(target, source) {\n    var merged = {};\n\n    var mergedKeys = Object.keys(Object.assign({}, target, source));\n\n    mergedKeys.forEach(function (key) {\n      if (target[key] === undefined && source[key] !== undefined) {\n        merged[key] = source[key];\n      } else if (target[key] !== undefined && source[key] === undefined) {\n        merged[key] = target[key];\n      } else if (target[key] !== undefined && source[key] !== undefined) {\n        if (target[key] === source[key]) {\n          merged[key] = target[key];\n        } else if (!Array.isArray(target[key]) && _typeof(target[key]) === 'object' && !Array.isArray(source[key]) && _typeof(source[key]) === 'object') {\n          merged[key] = merge(target[key], source[key]);\n        } else {\n          merged[key] = [].concat(target[key], source[key]);\n        }\n      }\n    });\n\n    return merged;\n  };\n\n  if (!items) {\n    return this;\n  }\n\n  if (items.constructor.name === 'Collection') {\n    return new this.constructor(merge(this.items, items.all()));\n  }\n\n  return new this.constructor(merge(this.items, items));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mergeRecursive.js?");

/***/ }),

/***/ "./dist/methods/min.js":
/*!*****************************!*\
  !*** ./dist/methods/min.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nfunction _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }\n\nmodule.exports = function min(key) {\n  if (key !== undefined) {\n    var filtered = this.items.filter(function (item) {\n      return item[key] !== undefined;\n    });\n\n    return Math.min.apply(Math, _toConsumableArray(filtered.map(function (item) {\n      return item[key];\n    })));\n  }\n\n  return Math.min.apply(Math, _toConsumableArray(this.items));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/min.js?");

/***/ }),

/***/ "./dist/methods/mode.js":
/*!******************************!*\
  !*** ./dist/methods/mode.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function mode(key) {\n  var values = [];\n  var highestCount = 1;\n\n  if (!this.items.length) {\n    return null;\n  }\n\n  this.items.forEach(function (item) {\n    var tempValues = values.filter(function (value) {\n      if (key !== undefined) {\n        return value.key === item[key];\n      }\n\n      return value.key === item;\n    });\n\n    if (!tempValues.length) {\n      if (key !== undefined) {\n        values.push({ key: item[key], count: 1 });\n      } else {\n        values.push({ key: item, count: 1 });\n      }\n    } else {\n      tempValues[0].count += 1;\n      var count = tempValues[0].count;\n\n\n      if (count > highestCount) {\n        highestCount = count;\n      }\n    }\n  });\n\n  return values.filter(function (value) {\n    return value.count === highestCount;\n  }).map(function (value) {\n    return value.key;\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/mode.js?");

/***/ }),

/***/ "./dist/methods/nth.js":
/*!*****************************!*\
  !*** ./dist/methods/nth.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nmodule.exports = function nth(n) {\n  var offset = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 0;\n\n  var items = values(this.items);\n\n  var collection = items.slice(offset).filter(function (item, index) {\n    return index % n === 0;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/nth.js?");

/***/ }),

/***/ "./dist/methods/only.js":
/*!******************************!*\
  !*** ./dist/methods/only.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar variadic = __webpack_require__(/*! ../helpers/variadic */ \"./dist/helpers/variadic.js\");\n\nmodule.exports = function only() {\n  var _this = this;\n\n  for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {\n    args[_key] = arguments[_key];\n  }\n\n  var properties = variadic(args);\n\n  if (Array.isArray(this.items)) {\n    var _collection = this.items.filter(function (item) {\n      return properties.indexOf(item) !== -1;\n    });\n\n    return new this.constructor(_collection);\n  }\n\n  var collection = {};\n\n  Object.keys(this.items).forEach(function (prop) {\n    if (properties.indexOf(prop) !== -1) {\n      collection[prop] = _this.items[prop];\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/only.js?");

/***/ }),

/***/ "./dist/methods/pad.js":
/*!*****************************!*\
  !*** ./dist/methods/pad.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar clone = __webpack_require__(/*! ../helpers/clone */ \"./dist/helpers/clone.js\");\n\nmodule.exports = function pad(size, value) {\n  var abs = Math.abs(size);\n  var count = this.count();\n\n  if (abs <= count) {\n    return this;\n  }\n\n  var diff = abs - count;\n  var items = clone(this.items);\n  var isArray = Array.isArray(this.items);\n  var prepend = size < 0;\n\n  for (var iterator = 0; iterator < diff;) {\n    if (!isArray) {\n      if (items[iterator] !== undefined) {\n        diff += 1;\n      } else {\n        items[iterator] = value;\n      }\n    } else if (prepend) {\n      items.unshift(value);\n    } else {\n      items.push(value);\n    }\n\n    iterator += 1;\n  }\n\n  return new this.constructor(items);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/pad.js?");

/***/ }),

/***/ "./dist/methods/partition.js":
/*!***********************************!*\
  !*** ./dist/methods/partition.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function partition(fn) {\n  var _this = this;\n\n  var arrays = void 0;\n\n  if (Array.isArray(this.items)) {\n    arrays = [new this.constructor([]), new this.constructor([])];\n\n    this.items.forEach(function (item) {\n      if (fn(item) === true) {\n        arrays[0].push(item);\n      } else {\n        arrays[1].push(item);\n      }\n    });\n  } else {\n    arrays = [new this.constructor({}), new this.constructor({})];\n\n    Object.keys(this.items).forEach(function (prop) {\n      var value = _this.items[prop];\n\n      if (fn(value) === true) {\n        arrays[0].put(prop, value);\n      } else {\n        arrays[1].put(prop, value);\n      }\n    });\n  }\n\n  return new this.constructor(arrays);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/partition.js?");

/***/ }),

/***/ "./dist/methods/pipe.js":
/*!******************************!*\
  !*** ./dist/methods/pipe.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function pipe(fn) {\n  return fn(this);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/pipe.js?");

/***/ }),

/***/ "./dist/methods/pluck.js":
/*!*******************************!*\
  !*** ./dist/methods/pluck.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nvar buildKeyPathMap = function buildKeyPathMap(items) {\n  var keyPaths = {};\n\n  items.forEach(function (item, index) {\n    function buildKeyPath(val, keyPath) {\n      if ((typeof val === 'undefined' ? 'undefined' : _typeof(val)) === 'object') {\n        Object.keys(val).forEach(function (prop) {\n          buildKeyPath(val[prop], keyPath + '.' + prop);\n        });\n      }\n\n      keyPaths[keyPath] = val;\n    }\n\n    buildKeyPath(item, index);\n  });\n\n  return keyPaths;\n};\n\nmodule.exports = function pluck(value, key) {\n  if (value.indexOf('*') !== -1) {\n    var keyPathMap = buildKeyPathMap(this.items);\n\n    var keyMatches = [];\n\n    if (key !== undefined) {\n      var keyRegex = new RegExp('0.' + key, 'g');\n      var keyNumberOfLevels = ('0.' + key).split('.').length;\n\n      Object.keys(keyPathMap).forEach(function (k) {\n        var matchingKey = k.match(keyRegex);\n\n        if (matchingKey) {\n          var match = matchingKey[0];\n\n          if (match.split('.').length === keyNumberOfLevels) {\n            keyMatches.push(keyPathMap[match]);\n          }\n        }\n      });\n    }\n\n    var valueMatches = [];\n    var valueRegex = new RegExp('0.' + value, 'g');\n    var valueNumberOfLevels = ('0.' + value).split('.').length;\n\n    Object.keys(keyPathMap).forEach(function (k) {\n      var matchingValue = k.match(valueRegex);\n\n      if (matchingValue) {\n        var match = matchingValue[0];\n\n        if (match.split('.').length === valueNumberOfLevels) {\n          valueMatches.push(keyPathMap[match]);\n        }\n      }\n    });\n\n    if (key !== undefined) {\n      var collection = {};\n\n      this.items.forEach(function (item, index) {\n        collection[keyMatches[index] || ''] = valueMatches;\n      });\n\n      return new this.constructor(collection);\n    }\n\n    return new this.constructor([valueMatches]);\n  }\n\n  if (key !== undefined) {\n    var _collection = {};\n\n    this.items.forEach(function (item) {\n      if (nestedValue(item, value) !== undefined) {\n        _collection[item[key] || ''] = nestedValue(item, value);\n      } else {\n        _collection[item[key] || ''] = null;\n      }\n    });\n\n    return new this.constructor(_collection);\n  }\n\n  return this.map(function (item) {\n    if (nestedValue(item, value) !== undefined) {\n      return nestedValue(item, value);\n    }\n\n    return null;\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/pluck.js?");

/***/ }),

/***/ "./dist/methods/pop.js":
/*!*****************************!*\
  !*** ./dist/methods/pop.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function pop() {\n  if (Array.isArray(this.items)) {\n    return this.items.pop();\n  }\n\n  var keys = Object.keys(this.items);\n  var key = keys[keys.length - 1];\n  var last = this.items[key];\n\n  delete this.items[key];\n\n  return last;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/pop.js?");

/***/ }),

/***/ "./dist/methods/prepend.js":
/*!*********************************!*\
  !*** ./dist/methods/prepend.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function prepend(value, key) {\n  if (key !== undefined) {\n    return this.put(key, value);\n  }\n\n  this.items.unshift(value);\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/prepend.js?");

/***/ }),

/***/ "./dist/methods/pull.js":
/*!******************************!*\
  !*** ./dist/methods/pull.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function pull(key, defaultValue) {\n  var returnValue = this.items[key] || null;\n\n  if (!returnValue && defaultValue !== undefined) {\n    if (isFunction(defaultValue)) {\n      returnValue = defaultValue();\n    } else {\n      returnValue = defaultValue;\n    }\n  }\n\n  delete this.items[key];\n\n  return returnValue;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/pull.js?");

/***/ }),

/***/ "./dist/methods/push.js":
/*!******************************!*\
  !*** ./dist/methods/push.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function push() {\n  var _items;\n\n  (_items = this.items).push.apply(_items, arguments);\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/push.js?");

/***/ }),

/***/ "./dist/methods/put.js":
/*!*****************************!*\
  !*** ./dist/methods/put.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function put(key, value) {\n  this.items[key] = value;\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/put.js?");

/***/ }),

/***/ "./dist/methods/random.js":
/*!********************************!*\
  !*** ./dist/methods/random.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nmodule.exports = function random() {\n  var length = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : null;\n\n  var items = values(this.items);\n\n  var collection = new this.constructor(items).shuffle();\n\n  // If not a length was specified\n  if (length !== parseInt(length, 10)) {\n    return collection.first();\n  }\n\n  return collection.take(length);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/random.js?");

/***/ }),

/***/ "./dist/methods/reduce.js":
/*!********************************!*\
  !*** ./dist/methods/reduce.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function reduce(fn, carry) {\n  var _this = this;\n\n  var reduceCarry = null;\n\n  if (carry !== undefined) {\n    reduceCarry = carry;\n  }\n\n  if (Array.isArray(this.items)) {\n    this.items.forEach(function (item) {\n      reduceCarry = fn(reduceCarry, item);\n    });\n  } else {\n    Object.keys(this.items).forEach(function (key) {\n      reduceCarry = fn(reduceCarry, _this.items[key], key);\n    });\n  }\n\n  return reduceCarry;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/reduce.js?");

/***/ }),

/***/ "./dist/methods/reject.js":
/*!********************************!*\
  !*** ./dist/methods/reject.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function reject(fn) {\n  return new this.constructor(this.items).filter(function (item) {\n    return !fn(item);\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/reject.js?");

/***/ }),

/***/ "./dist/methods/replace.js":
/*!*********************************!*\
  !*** ./dist/methods/replace.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function replace(items) {\n  if (!items) {\n    return this;\n  }\n\n  if (Array.isArray(items)) {\n    var _replaced = this.items.map(function (value, index) {\n      return items[index] || value;\n    });\n\n    return new this.constructor(_replaced);\n  }\n\n  if (items.constructor.name === 'Collection') {\n    var _replaced2 = Object.assign({}, this.items, items.all());\n\n    return new this.constructor(_replaced2);\n  }\n\n  var replaced = Object.assign({}, this.items, items);\n\n  return new this.constructor(replaced);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/replace.js?");

/***/ }),

/***/ "./dist/methods/replaceRecursive.js":
/*!******************************************!*\
  !*** ./dist/methods/replaceRecursive.js ***!
  \******************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function replaceRecursive(items) {\n  var replace = function replace(target, source) {\n    var replaced = Object.assign({}, target);\n\n    var mergedKeys = Object.keys(Object.assign({}, target, source));\n\n    mergedKeys.forEach(function (key) {\n      if (!Array.isArray(source[key]) && _typeof(source[key]) === 'object') {\n        replaced[key] = replace(target[key], source[key]);\n      } else if (target[key] === undefined && source[key] !== undefined) {\n        if (_typeof(target[key]) === 'object') {\n          replaced[key] = Object.assign({}, source[key]);\n        } else {\n          replaced[key] = source[key];\n        }\n      } else if (target[key] !== undefined && source[key] === undefined) {\n        if (_typeof(target[key]) === 'object') {\n          replaced[key] = Object.assign({}, target[key]);\n        } else {\n          replaced[key] = target[key];\n        }\n      } else if (target[key] !== undefined && source[key] !== undefined) {\n        if (_typeof(source[key]) === 'object') {\n          replaced[key] = Object.assign({}, source[key]);\n        } else {\n          replaced[key] = source[key];\n        }\n      }\n    });\n\n    return replaced;\n  };\n\n  if (!items) {\n    return this;\n  }\n\n  if (!Array.isArray(items) && (typeof items === 'undefined' ? 'undefined' : _typeof(items)) !== 'object') {\n    return new this.constructor(replace(this.items, [items]));\n  }\n\n  if (items.constructor.name === 'Collection') {\n    return new this.constructor(replace(this.items, items.all()));\n  }\n\n  return new this.constructor(replace(this.items, items));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/replaceRecursive.js?");

/***/ }),

/***/ "./dist/methods/reverse.js":
/*!*********************************!*\
  !*** ./dist/methods/reverse.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function reverse() {\n  var collection = [].concat(this.items).reverse();\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/reverse.js?");

/***/ }),

/***/ "./dist/methods/search.js":
/*!********************************!*\
  !*** ./dist/methods/search.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function search(valueOrFunction, strict) {\n  var _this = this;\n\n  var valueFn = valueOrFunction;\n\n  if (isFunction(valueOrFunction)) {\n    valueFn = this.items.find(function (value, key) {\n      return valueOrFunction(value, key);\n    });\n  }\n\n  var index = false;\n\n  if (Array.isArray(this.items)) {\n    var itemKey = this.items.filter(function (item) {\n      if (strict === true) {\n        return item === valueFn;\n      }\n\n      return item === Number(valueFn) || item === String(valueFn);\n    })[0];\n\n    index = this.items.indexOf(itemKey);\n  } else {\n    return Object.keys(this.items).filter(function (prop) {\n      if (strict === true) {\n        return _this.items[prop] === valueFn;\n      }\n\n      return _this.items[prop] === Number(valueFn) || _this.items[prop] === valueFn.toString();\n    })[0] || false;\n  }\n\n  if (index === -1) {\n    return false;\n  }\n\n  return index;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/search.js?");

/***/ }),

/***/ "./dist/methods/shift.js":
/*!*******************************!*\
  !*** ./dist/methods/shift.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function shift() {\n  if (Array.isArray(this.items) && this.items.length) {\n    return this.items.shift();\n  }\n\n  if (Object.keys(this.items).length) {\n    var key = Object.keys(this.items)[0];\n    var value = this.items[key];\n    delete this.items[key];\n\n    return value;\n  }\n\n  return null;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/shift.js?");

/***/ }),

/***/ "./dist/methods/shuffle.js":
/*!*********************************!*\
  !*** ./dist/methods/shuffle.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nmodule.exports = function shuffle() {\n  var items = values(this.items);\n\n  var j = void 0;\n  var x = void 0;\n  var i = void 0;\n\n  for (i = items.length; i; i -= 1) {\n    j = Math.floor(Math.random() * i);\n    x = items[i - 1];\n    items[i - 1] = items[j];\n    items[j] = x;\n  }\n\n  this.items = items;\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/shuffle.js?");

/***/ }),

/***/ "./dist/methods/slice.js":
/*!*******************************!*\
  !*** ./dist/methods/slice.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function slice(remove, limit) {\n  var collection = this.items.slice(remove);\n\n  if (limit !== undefined) {\n    collection = collection.slice(0, limit);\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/slice.js?");

/***/ }),

/***/ "./dist/methods/sort.js":
/*!******************************!*\
  !*** ./dist/methods/sort.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function sort(fn) {\n  var collection = [].concat(this.items);\n\n  if (fn === undefined) {\n    if (this.every(function (item) {\n      return typeof item === 'number';\n    })) {\n      collection.sort(function (a, b) {\n        return a - b;\n      });\n    } else {\n      collection.sort();\n    }\n  } else {\n    collection.sort(fn);\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sort.js?");

/***/ }),

/***/ "./dist/methods/sortBy.js":
/*!********************************!*\
  !*** ./dist/methods/sortBy.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function sortBy(valueOrFunction) {\n  var collection = [].concat(this.items);\n  var getValue = function getValue(item) {\n    if (isFunction(valueOrFunction)) {\n      return valueOrFunction(item);\n    }\n\n    return nestedValue(item, valueOrFunction);\n  };\n\n  collection.sort(function (a, b) {\n    var valueA = getValue(a);\n    var valueB = getValue(b);\n\n    if (valueA === null || valueA === undefined) {\n      return 1;\n    }\n    if (valueB === null || valueB === undefined) {\n      return -1;\n    }\n\n    if (valueA < valueB) {\n      return -1;\n    }\n    if (valueA > valueB) {\n      return 1;\n    }\n\n    return 0;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sortBy.js?");

/***/ }),

/***/ "./dist/methods/sortByDesc.js":
/*!************************************!*\
  !*** ./dist/methods/sortByDesc.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function sortByDesc(valueOrFunction) {\n  return this.sortBy(valueOrFunction).reverse();\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sortByDesc.js?");

/***/ }),

/***/ "./dist/methods/sortKeys.js":
/*!**********************************!*\
  !*** ./dist/methods/sortKeys.js ***!
  \**********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function sortKeys() {\n  var _this = this;\n\n  var ordered = {};\n\n  Object.keys(this.items).sort().forEach(function (key) {\n    ordered[key] = _this.items[key];\n  });\n\n  return new this.constructor(ordered);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sortKeys.js?");

/***/ }),

/***/ "./dist/methods/sortKeysDesc.js":
/*!**************************************!*\
  !*** ./dist/methods/sortKeysDesc.js ***!
  \**************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function sortKeysDesc() {\n  var _this = this;\n\n  var ordered = {};\n\n  Object.keys(this.items).sort().reverse().forEach(function (key) {\n    ordered[key] = _this.items[key];\n  });\n\n  return new this.constructor(ordered);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sortKeysDesc.js?");

/***/ }),

/***/ "./dist/methods/splice.js":
/*!********************************!*\
  !*** ./dist/methods/splice.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function splice(index, limit, replace) {\n  var slicedCollection = this.slice(index, limit);\n\n  this.items = this.diff(slicedCollection.all()).all();\n\n  if (Array.isArray(replace)) {\n    for (var iterator = 0, length = replace.length; iterator < length; iterator += 1) {\n      this.items.splice(index + iterator, 0, replace[iterator]);\n    }\n  }\n\n  return slicedCollection;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/splice.js?");

/***/ }),

/***/ "./dist/methods/split.js":
/*!*******************************!*\
  !*** ./dist/methods/split.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function split(numberOfGroups) {\n  var itemsPerGroup = Math.round(this.items.length / numberOfGroups);\n\n  var items = JSON.parse(JSON.stringify(this.items));\n  var collection = [];\n\n  for (var iterator = 0; iterator < numberOfGroups; iterator += 1) {\n    collection.push(new this.constructor(items.splice(0, itemsPerGroup)));\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/split.js?");

/***/ }),

/***/ "./dist/methods/sum.js":
/*!*****************************!*\
  !*** ./dist/methods/sum.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function sum(key) {\n  var items = values(this.items);\n\n  var total = 0;\n\n  if (key === undefined) {\n    for (var i = 0, length = items.length; i < length; i += 1) {\n      total += items[i];\n    }\n  } else if (isFunction(key)) {\n    for (var _i = 0, _length = items.length; _i < _length; _i += 1) {\n      total += key(items[_i]);\n    }\n  } else {\n    for (var _i2 = 0, _length2 = items.length; _i2 < _length2; _i2 += 1) {\n      total += items[_i2][key];\n    }\n  }\n\n  return total;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/sum.js?");

/***/ }),

/***/ "./dist/methods/symbol.iterator.js":
/*!*****************************************!*\
  !*** ./dist/methods/symbol.iterator.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function SymbolIterator() {\n  var _this = this;\n\n  var index = -1;\n\n  return {\n    next: function next() {\n      index += 1;\n\n      return {\n        value: _this.items[index],\n        done: index >= _this.items.length\n      };\n    }\n  };\n};\n\n//# sourceURL=webpack://collect/./dist/methods/symbol.iterator.js?");

/***/ }),

/***/ "./dist/methods/take.js":
/*!******************************!*\
  !*** ./dist/methods/take.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function take(length) {\n  var _this = this;\n\n  if (!Array.isArray(this.items) && _typeof(this.items) === 'object') {\n    var keys = Object.keys(this.items);\n    var slicedKeys = void 0;\n\n    if (length < 0) {\n      slicedKeys = keys.slice(length);\n    } else {\n      slicedKeys = keys.slice(0, length);\n    }\n\n    var collection = {};\n\n    keys.forEach(function (prop) {\n      if (slicedKeys.indexOf(prop) !== -1) {\n        collection[prop] = _this.items[prop];\n      }\n    });\n\n    return new this.constructor(collection);\n  }\n\n  if (length < 0) {\n    return new this.constructor(this.items.slice(length));\n  }\n\n  return new this.constructor(this.items.slice(0, length));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/take.js?");

/***/ }),

/***/ "./dist/methods/tap.js":
/*!*****************************!*\
  !*** ./dist/methods/tap.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function tap(fn) {\n  fn(this);\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/tap.js?");

/***/ }),

/***/ "./dist/methods/times.js":
/*!*******************************!*\
  !*** ./dist/methods/times.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function times(n, fn) {\n  for (var iterator = 1; iterator <= n; iterator += 1) {\n    this.items.push(fn(iterator));\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/times.js?");

/***/ }),

/***/ "./dist/methods/toArray.js":
/*!*********************************!*\
  !*** ./dist/methods/toArray.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function toArray() {\n  var collectionInstance = this.constructor;\n\n  function iterate(list, collection) {\n    var childCollection = [];\n\n    if (list instanceof collectionInstance) {\n      list.items.forEach(function (i) {\n        return iterate(i, childCollection);\n      });\n      collection.push(childCollection);\n    } else if (Array.isArray(list)) {\n      list.forEach(function (i) {\n        return iterate(i, childCollection);\n      });\n      collection.push(childCollection);\n    } else {\n      collection.push(list);\n    }\n  }\n\n  if (Array.isArray(this.items)) {\n    var collection = [];\n\n    this.items.forEach(function (items) {\n      iterate(items, collection);\n    });\n\n    return collection;\n  }\n\n  return this.values().all();\n};\n\n//# sourceURL=webpack://collect/./dist/methods/toArray.js?");

/***/ }),

/***/ "./dist/methods/toJson.js":
/*!********************************!*\
  !*** ./dist/methods/toJson.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function toJson() {\n  if (_typeof(this.items) === 'object' && !Array.isArray(this.items)) {\n    return JSON.stringify(this.all());\n  }\n\n  return JSON.stringify(this.toArray());\n};\n\n//# sourceURL=webpack://collect/./dist/methods/toJson.js?");

/***/ }),

/***/ "./dist/methods/transform.js":
/*!***********************************!*\
  !*** ./dist/methods/transform.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function transform(fn) {\n  var _this = this;\n\n  if (Array.isArray(this.items)) {\n    this.items = this.items.map(fn);\n  } else {\n    var collection = {};\n\n    Object.keys(this.items).forEach(function (key) {\n      collection[key] = fn(_this.items[key], key);\n    });\n\n    this.items = collection;\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/transform.js?");

/***/ }),

/***/ "./dist/methods/union.js":
/*!*******************************!*\
  !*** ./dist/methods/union.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function union(object) {\n  var _this = this;\n\n  var collection = JSON.parse(JSON.stringify(this.items));\n\n  Object.keys(object).forEach(function (prop) {\n    if (_this.items[prop] === undefined) {\n      collection[prop] = object[prop];\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/union.js?");

/***/ }),

/***/ "./dist/methods/unique.js":
/*!********************************!*\
  !*** ./dist/methods/unique.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _require = __webpack_require__(/*! ../helpers/is */ \"./dist/helpers/is.js\"),\n    isFunction = _require.isFunction;\n\nmodule.exports = function unique(key) {\n  var collection = void 0;\n\n  if (key === undefined) {\n    collection = this.items.filter(function (element, index, self) {\n      return self.indexOf(element) === index;\n    });\n  } else {\n    collection = [];\n\n    var usedKeys = [];\n\n    for (var iterator = 0, length = this.items.length; iterator < length; iterator += 1) {\n      var uniqueKey = void 0;\n      if (isFunction(key)) {\n        uniqueKey = key(this.items[iterator]);\n      } else {\n        uniqueKey = this.items[iterator][key];\n      }\n\n      if (usedKeys.indexOf(uniqueKey) === -1) {\n        collection.push(this.items[iterator]);\n        usedKeys.push(uniqueKey);\n      }\n    }\n  }\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/unique.js?");

/***/ }),

/***/ "./dist/methods/unless.js":
/*!********************************!*\
  !*** ./dist/methods/unless.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function when(value, fn, defaultFn) {\n  if (!value) {\n    fn(this);\n  } else {\n    defaultFn(this);\n  }\n};\n\n//# sourceURL=webpack://collect/./dist/methods/unless.js?");

/***/ }),

/***/ "./dist/methods/unwrap.js":
/*!********************************!*\
  !*** ./dist/methods/unwrap.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function unwrap(value) {\n  if (value instanceof this.constructor) {\n    return value.all();\n  }\n\n  return value;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/unwrap.js?");

/***/ }),

/***/ "./dist/methods/values.js":
/*!********************************!*\
  !*** ./dist/methods/values.js ***!
  \********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar getValues = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\n\nmodule.exports = function values() {\n  return new this.constructor(getValues(this.items));\n};\n\n//# sourceURL=webpack://collect/./dist/methods/values.js?");

/***/ }),

/***/ "./dist/methods/when.js":
/*!******************************!*\
  !*** ./dist/methods/when.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function when(value, fn, defaultFn) {\n  if (value) {\n    return fn(this, value);\n  }\n\n  if (defaultFn) {\n    return defaultFn(this, value);\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/when.js?");

/***/ }),

/***/ "./dist/methods/whenEmpty.js":
/*!***********************************!*\
  !*** ./dist/methods/whenEmpty.js ***!
  \***********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function whenEmpty(fn, defaultFn) {\n  if (Array.isArray(this.items) && !this.items.length) {\n    return fn(this);\n  }if (!Object.keys(this.items).length) {\n    return fn(this);\n  }\n\n  if (defaultFn !== undefined) {\n    if (Array.isArray(this.items) && this.items.length) {\n      return defaultFn(this);\n    }if (Object.keys(this.items).length) {\n      return defaultFn(this);\n    }\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whenEmpty.js?");

/***/ }),

/***/ "./dist/methods/whenNotEmpty.js":
/*!**************************************!*\
  !*** ./dist/methods/whenNotEmpty.js ***!
  \**************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function whenNotEmpty(fn, defaultFn) {\n  if (Array.isArray(this.items) && this.items.length) {\n    return fn(this);\n  }if (Object.keys(this.items).length) {\n    return fn(this);\n  }\n\n  if (defaultFn !== undefined) {\n    if (Array.isArray(this.items) && !this.items.length) {\n      return defaultFn(this);\n    }if (!Object.keys(this.items).length) {\n      return defaultFn(this);\n    }\n  }\n\n  return this;\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whenNotEmpty.js?");

/***/ }),

/***/ "./dist/methods/where.js":
/*!*******************************!*\
  !*** ./dist/methods/where.js ***!
  \*******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar values = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nmodule.exports = function where(key, operator, value) {\n  var comparisonOperator = operator;\n  var comparisonValue = value;\n\n  var items = values(this.items);\n\n  if (operator === undefined || operator === true) {\n    return new this.constructor(items.filter(function (item) {\n      return nestedValue(item, key);\n    }));\n  }\n\n  if (operator === false) {\n    return new this.constructor(items.filter(function (item) {\n      return !nestedValue(item, key);\n    }));\n  }\n\n  if (value === undefined) {\n    comparisonValue = operator;\n    comparisonOperator = '===';\n  }\n\n  var collection = items.filter(function (item) {\n    switch (comparisonOperator) {\n      case '==':\n        return nestedValue(item, key) === Number(comparisonValue) || nestedValue(item, key) === comparisonValue.toString();\n\n      default:\n      case '===':\n        return nestedValue(item, key) === comparisonValue;\n\n      case '!=':\n      case '<>':\n        return nestedValue(item, key) !== Number(comparisonValue) && nestedValue(item, key) !== comparisonValue.toString();\n\n      case '!==':\n        return nestedValue(item, key) !== comparisonValue;\n\n      case '<':\n        return nestedValue(item, key) < comparisonValue;\n\n      case '<=':\n        return nestedValue(item, key) <= comparisonValue;\n\n      case '>':\n        return nestedValue(item, key) > comparisonValue;\n\n      case '>=':\n        return nestedValue(item, key) >= comparisonValue;\n    }\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/where.js?");

/***/ }),

/***/ "./dist/methods/whereBetween.js":
/*!**************************************!*\
  !*** ./dist/methods/whereBetween.js ***!
  \**************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function whereBetween(key, values) {\n  return this.where(key, '>=', values[0]).where(key, '<=', values[values.length - 1]);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whereBetween.js?");

/***/ }),

/***/ "./dist/methods/whereIn.js":
/*!*********************************!*\
  !*** ./dist/methods/whereIn.js ***!
  \*********************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar extractValues = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nmodule.exports = function whereIn(key, values) {\n  var items = extractValues(values);\n\n  var collection = this.items.filter(function (item) {\n    return items.indexOf(nestedValue(item, key)) !== -1;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whereIn.js?");

/***/ }),

/***/ "./dist/methods/whereInstanceOf.js":
/*!*****************************************!*\
  !*** ./dist/methods/whereInstanceOf.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function whereInstanceOf(type) {\n  return this.filter(function (item) {\n    return item instanceof type;\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whereInstanceOf.js?");

/***/ }),

/***/ "./dist/methods/whereNotBetween.js":
/*!*****************************************!*\
  !*** ./dist/methods/whereNotBetween.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nmodule.exports = function whereNotBetween(key, values) {\n  return this.filter(function (item) {\n    return nestedValue(item, key) < values[0] || nestedValue(item, key) > values[values.length - 1];\n  });\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whereNotBetween.js?");

/***/ }),

/***/ "./dist/methods/whereNotIn.js":
/*!************************************!*\
  !*** ./dist/methods/whereNotIn.js ***!
  \************************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar extractValues = __webpack_require__(/*! ../helpers/values */ \"./dist/helpers/values.js\");\nvar nestedValue = __webpack_require__(/*! ../helpers/nestedValue */ \"./dist/helpers/nestedValue.js\");\n\nmodule.exports = function whereNotIn(key, values) {\n  var items = extractValues(values);\n\n  var collection = this.items.filter(function (item) {\n    return items.indexOf(nestedValue(item, key)) === -1;\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/whereNotIn.js?");

/***/ }),

/***/ "./dist/methods/wrap.js":
/*!******************************!*\
  !*** ./dist/methods/wrap.js ***!
  \******************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nvar _typeof = typeof Symbol === \"function\" && typeof Symbol.iterator === \"symbol\" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === \"function\" && obj.constructor === Symbol && obj !== Symbol.prototype ? \"symbol\" : typeof obj; };\n\nmodule.exports = function wrap(value) {\n  if (value instanceof this.constructor) {\n    return value;\n  }\n\n  if ((typeof value === 'undefined' ? 'undefined' : _typeof(value)) === 'object') {\n    return new this.constructor(value);\n  }\n\n  return new this.constructor([value]);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/wrap.js?");

/***/ }),

/***/ "./dist/methods/zip.js":
/*!*****************************!*\
  !*** ./dist/methods/zip.js ***!
  \*****************************/
/*! no static exports found */
/***/ (function(module, exports, __webpack_require__) {

"use strict";
eval("\n\nmodule.exports = function zip(array) {\n  var _this = this;\n\n  var values = array;\n\n  if (values instanceof this.constructor) {\n    values = values.all();\n  }\n\n  var collection = this.items.map(function (item, index) {\n    return new _this.constructor([item, values[index]]);\n  });\n\n  return new this.constructor(collection);\n};\n\n//# sourceURL=webpack://collect/./dist/methods/zip.js?");

/***/ }),

/***/ "./node_modules/process/browser.js":
/*!*****************************************!*\
  !*** ./node_modules/process/browser.js ***!
  \*****************************************/
/*! no static exports found */
/***/ (function(module, exports) {

eval("// shim for using process in browser\nvar process = module.exports = {};\n\n// cached from whatever global is present so that test runners that stub it\n// don't break things.  But we need to wrap it in a try catch in case it is\n// wrapped in strict mode code which doesn't define any globals.  It's inside a\n// function because try/catches deoptimize in certain engines.\n\nvar cachedSetTimeout;\nvar cachedClearTimeout;\n\nfunction defaultSetTimout() {\n    throw new Error('setTimeout has not been defined');\n}\nfunction defaultClearTimeout () {\n    throw new Error('clearTimeout has not been defined');\n}\n(function () {\n    try {\n        if (typeof setTimeout === 'function') {\n            cachedSetTimeout = setTimeout;\n        } else {\n            cachedSetTimeout = defaultSetTimout;\n        }\n    } catch (e) {\n        cachedSetTimeout = defaultSetTimout;\n    }\n    try {\n        if (typeof clearTimeout === 'function') {\n            cachedClearTimeout = clearTimeout;\n        } else {\n            cachedClearTimeout = defaultClearTimeout;\n        }\n    } catch (e) {\n        cachedClearTimeout = defaultClearTimeout;\n    }\n} ())\nfunction runTimeout(fun) {\n    if (cachedSetTimeout === setTimeout) {\n        //normal enviroments in sane situations\n        return setTimeout(fun, 0);\n    }\n    // if setTimeout wasn't available but was latter defined\n    if ((cachedSetTimeout === defaultSetTimout || !cachedSetTimeout) && setTimeout) {\n        cachedSetTimeout = setTimeout;\n        return setTimeout(fun, 0);\n    }\n    try {\n        // when when somebody has screwed with setTimeout but no I.E. maddness\n        return cachedSetTimeout(fun, 0);\n    } catch(e){\n        try {\n            // When we are in I.E. but the script has been evaled so I.E. doesn't trust the global object when called normally\n            return cachedSetTimeout.call(null, fun, 0);\n        } catch(e){\n            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error\n            return cachedSetTimeout.call(this, fun, 0);\n        }\n    }\n\n\n}\nfunction runClearTimeout(marker) {\n    if (cachedClearTimeout === clearTimeout) {\n        //normal enviroments in sane situations\n        return clearTimeout(marker);\n    }\n    // if clearTimeout wasn't available but was latter defined\n    if ((cachedClearTimeout === defaultClearTimeout || !cachedClearTimeout) && clearTimeout) {\n        cachedClearTimeout = clearTimeout;\n        return clearTimeout(marker);\n    }\n    try {\n        // when when somebody has screwed with setTimeout but no I.E. maddness\n        return cachedClearTimeout(marker);\n    } catch (e){\n        try {\n            // When we are in I.E. but the script has been evaled so I.E. doesn't  trust the global object when called normally\n            return cachedClearTimeout.call(null, marker);\n        } catch (e){\n            // same as above but when it's a version of I.E. that must have the global object for 'this', hopfully our context correct otherwise it will throw a global error.\n            // Some versions of I.E. have different rules for clearTimeout vs setTimeout\n            return cachedClearTimeout.call(this, marker);\n        }\n    }\n\n\n\n}\nvar queue = [];\nvar draining = false;\nvar currentQueue;\nvar queueIndex = -1;\n\nfunction cleanUpNextTick() {\n    if (!draining || !currentQueue) {\n        return;\n    }\n    draining = false;\n    if (currentQueue.length) {\n        queue = currentQueue.concat(queue);\n    } else {\n        queueIndex = -1;\n    }\n    if (queue.length) {\n        drainQueue();\n    }\n}\n\nfunction drainQueue() {\n    if (draining) {\n        return;\n    }\n    var timeout = runTimeout(cleanUpNextTick);\n    draining = true;\n\n    var len = queue.length;\n    while(len) {\n        currentQueue = queue;\n        queue = [];\n        while (++queueIndex < len) {\n            if (currentQueue) {\n                currentQueue[queueIndex].run();\n            }\n        }\n        queueIndex = -1;\n        len = queue.length;\n    }\n    currentQueue = null;\n    draining = false;\n    runClearTimeout(timeout);\n}\n\nprocess.nextTick = function (fun) {\n    var args = new Array(arguments.length - 1);\n    if (arguments.length > 1) {\n        for (var i = 1; i < arguments.length; i++) {\n            args[i - 1] = arguments[i];\n        }\n    }\n    queue.push(new Item(fun, args));\n    if (queue.length === 1 && !draining) {\n        runTimeout(drainQueue);\n    }\n};\n\n// v8 likes predictible objects\nfunction Item(fun, array) {\n    this.fun = fun;\n    this.array = array;\n}\nItem.prototype.run = function () {\n    this.fun.apply(null, this.array);\n};\nprocess.title = 'browser';\nprocess.browser = true;\nprocess.env = {};\nprocess.argv = [];\nprocess.version = ''; // empty string to avoid regexp issues\nprocess.versions = {};\n\nfunction noop() {}\n\nprocess.on = noop;\nprocess.addListener = noop;\nprocess.once = noop;\nprocess.off = noop;\nprocess.removeListener = noop;\nprocess.removeAllListeners = noop;\nprocess.emit = noop;\nprocess.prependListener = noop;\nprocess.prependOnceListener = noop;\n\nprocess.listeners = function (name) { return [] }\n\nprocess.binding = function (name) {\n    throw new Error('process.binding is not supported');\n};\n\nprocess.cwd = function () { return '/' };\nprocess.chdir = function (dir) {\n    throw new Error('process.chdir is not supported');\n};\nprocess.umask = function() { return 0; };\n\n\n//# sourceURL=webpack://collect/./node_modules/process/browser.js?");

/***/ })

/******/ });
/**
 * @fileoverview
 * - Using the 'QRCode for Javascript library'
 * - Fixed dataset of 'QRCode for Javascript library' for support full-spec.
 * - this library has no dependencies.
 * 
 * @author davidshimjs
 * @see <a href="http://www.d-project.com/" target="_blank">http://www.d-project.com/</a>
 * @see <a href="http://jeromeetienne.github.com/jquery-qrcode/" target="_blank">http://jeromeetienne.github.com/jquery-qrcode/</a>
 */
var QRCode;

(function () {
	//---------------------------------------------------------------------
	// QRCode for JavaScript
	//
	// Copyright (c) 2009 Kazuhiko Arase
	//
	// URL: http://www.d-project.com/
	//
	// Licensed under the MIT license:
	//   http://www.opensource.org/licenses/mit-license.php
	//
	// The word "QR Code" is registered trademark of 
	// DENSO WAVE INCORPORATED
	//   http://www.denso-wave.com/qrcode/faqpatent-e.html
	//
	//---------------------------------------------------------------------
	function QR8bitByte(data) {
		this.mode = QRMode.MODE_8BIT_BYTE;
		this.data = data;
		this.parsedData = [];

		// Added to support UTF-8 Characters
		for (var i = 0, l = this.data.length; i < l; i++) {
			var byteArray = [];
			var code = this.data.charCodeAt(i);

			if (code > 0x10000) {
				byteArray[0] = 0xF0 | ((code & 0x1C0000) >>> 18);
				byteArray[1] = 0x80 | ((code & 0x3F000) >>> 12);
				byteArray[2] = 0x80 | ((code & 0xFC0) >>> 6);
				byteArray[3] = 0x80 | (code & 0x3F);
			} else if (code > 0x800) {
				byteArray[0] = 0xE0 | ((code & 0xF000) >>> 12);
				byteArray[1] = 0x80 | ((code & 0xFC0) >>> 6);
				byteArray[2] = 0x80 | (code & 0x3F);
			} else if (code > 0x80) {
				byteArray[0] = 0xC0 | ((code & 0x7C0) >>> 6);
				byteArray[1] = 0x80 | (code & 0x3F);
			} else {
				byteArray[0] = code;
			}

			this.parsedData.push(byteArray);
		}

		this.parsedData = Array.prototype.concat.apply([], this.parsedData);

		if (this.parsedData.length != this.data.length) {
			this.parsedData.unshift(191);
			this.parsedData.unshift(187);
			this.parsedData.unshift(239);
		}
	}

	QR8bitByte.prototype = {
		getLength: function (buffer) {
			return this.parsedData.length;
		},
		write: function (buffer) {
			for (var i = 0, l = this.parsedData.length; i < l; i++) {
				buffer.put(this.parsedData[i], 8);
			}
		}
	};

	function QRCodeModel(typeNumber, errorCorrectLevel) {
		this.typeNumber = typeNumber;
		this.errorCorrectLevel = errorCorrectLevel;
		this.modules = null;
		this.moduleCount = 0;
		this.dataCache = null;
		this.dataList = [];
	}

	QRCodeModel.prototype={addData:function(data){var newData=new QR8bitByte(data);this.dataList.push(newData);this.dataCache=null;},isDark:function(row,col){if(row<0||this.moduleCount<=row||col<0||this.moduleCount<=col){throw new Error(row+","+col);}
	return this.modules[row][col];},getModuleCount:function(){return this.moduleCount;},make:function(){this.makeImpl(false,this.getBestMaskPattern());},makeImpl:function(test,maskPattern){this.moduleCount=this.typeNumber*4+17;this.modules=new Array(this.moduleCount);for(var row=0;row<this.moduleCount;row++){this.modules[row]=new Array(this.moduleCount);for(var col=0;col<this.moduleCount;col++){this.modules[row][col]=null;}}
	this.setupPositionProbePattern(0,0);this.setupPositionProbePattern(this.moduleCount-7,0);this.setupPositionProbePattern(0,this.moduleCount-7);this.setupPositionAdjustPattern();this.setupTimingPattern();this.setupTypeInfo(test,maskPattern);if(this.typeNumber>=7){this.setupTypeNumber(test);}
	if(this.dataCache==null){this.dataCache=QRCodeModel.createData(this.typeNumber,this.errorCorrectLevel,this.dataList);}
	this.mapData(this.dataCache,maskPattern);},setupPositionProbePattern:function(row,col){for(var r=-1;r<=7;r++){if(row+r<=-1||this.moduleCount<=row+r)continue;for(var c=-1;c<=7;c++){if(col+c<=-1||this.moduleCount<=col+c)continue;if((0<=r&&r<=6&&(c==0||c==6))||(0<=c&&c<=6&&(r==0||r==6))||(2<=r&&r<=4&&2<=c&&c<=4)){this.modules[row+r][col+c]=true;}else{this.modules[row+r][col+c]=false;}}}},getBestMaskPattern:function(){var minLostPoint=0;var pattern=0;for(var i=0;i<8;i++){this.makeImpl(true,i);var lostPoint=QRUtil.getLostPoint(this);if(i==0||minLostPoint>lostPoint){minLostPoint=lostPoint;pattern=i;}}
	return pattern;},createMovieClip:function(target_mc,instance_name,depth){var qr_mc=target_mc.createEmptyMovieClip(instance_name,depth);var cs=1;this.make();for(var row=0;row<this.modules.length;row++){var y=row*cs;for(var col=0;col<this.modules[row].length;col++){var x=col*cs;var dark=this.modules[row][col];if(dark){qr_mc.beginFill(0,100);qr_mc.moveTo(x,y);qr_mc.lineTo(x+cs,y);qr_mc.lineTo(x+cs,y+cs);qr_mc.lineTo(x,y+cs);qr_mc.endFill();}}}
	return qr_mc;},setupTimingPattern:function(){for(var r=8;r<this.moduleCount-8;r++){if(this.modules[r][6]!=null){continue;}
	this.modules[r][6]=(r%2==0);}
	for(var c=8;c<this.moduleCount-8;c++){if(this.modules[6][c]!=null){continue;}
	this.modules[6][c]=(c%2==0);}},setupPositionAdjustPattern:function(){var pos=QRUtil.getPatternPosition(this.typeNumber);for(var i=0;i<pos.length;i++){for(var j=0;j<pos.length;j++){var row=pos[i];var col=pos[j];if(this.modules[row][col]!=null){continue;}
	for(var r=-2;r<=2;r++){for(var c=-2;c<=2;c++){if(r==-2||r==2||c==-2||c==2||(r==0&&c==0)){this.modules[row+r][col+c]=true;}else{this.modules[row+r][col+c]=false;}}}}}},setupTypeNumber:function(test){var bits=QRUtil.getBCHTypeNumber(this.typeNumber);for(var i=0;i<18;i++){var mod=(!test&&((bits>>i)&1)==1);this.modules[Math.floor(i/3)][i%3+this.moduleCount-8-3]=mod;}
	for(var i=0;i<18;i++){var mod=(!test&&((bits>>i)&1)==1);this.modules[i%3+this.moduleCount-8-3][Math.floor(i/3)]=mod;}},setupTypeInfo:function(test,maskPattern){var data=(this.errorCorrectLevel<<3)|maskPattern;var bits=QRUtil.getBCHTypeInfo(data);for(var i=0;i<15;i++){var mod=(!test&&((bits>>i)&1)==1);if(i<6){this.modules[i][8]=mod;}else if(i<8){this.modules[i+1][8]=mod;}else{this.modules[this.moduleCount-15+i][8]=mod;}}
	for(var i=0;i<15;i++){var mod=(!test&&((bits>>i)&1)==1);if(i<8){this.modules[8][this.moduleCount-i-1]=mod;}else if(i<9){this.modules[8][15-i-1+1]=mod;}else{this.modules[8][15-i-1]=mod;}}
	this.modules[this.moduleCount-8][8]=(!test);},mapData:function(data,maskPattern){var inc=-1;var row=this.moduleCount-1;var bitIndex=7;var byteIndex=0;for(var col=this.moduleCount-1;col>0;col-=2){if(col==6)col--;while(true){for(var c=0;c<2;c++){if(this.modules[row][col-c]==null){var dark=false;if(byteIndex<data.length){dark=(((data[byteIndex]>>>bitIndex)&1)==1);}
	var mask=QRUtil.getMask(maskPattern,row,col-c);if(mask){dark=!dark;}
	this.modules[row][col-c]=dark;bitIndex--;if(bitIndex==-1){byteIndex++;bitIndex=7;}}}
	row+=inc;if(row<0||this.moduleCount<=row){row-=inc;inc=-inc;break;}}}}};QRCodeModel.PAD0=0xEC;QRCodeModel.PAD1=0x11;QRCodeModel.createData=function(typeNumber,errorCorrectLevel,dataList){var rsBlocks=QRRSBlock.getRSBlocks(typeNumber,errorCorrectLevel);var buffer=new QRBitBuffer();for(var i=0;i<dataList.length;i++){var data=dataList[i];buffer.put(data.mode,4);buffer.put(data.getLength(),QRUtil.getLengthInBits(data.mode,typeNumber));data.write(buffer);}
	var totalDataCount=0;for(var i=0;i<rsBlocks.length;i++){totalDataCount+=rsBlocks[i].dataCount;}
	if(buffer.getLengthInBits()>totalDataCount*8){throw new Error("code length overflow. ("
	+buffer.getLengthInBits()
	+">"
	+totalDataCount*8
	+")");}
	if(buffer.getLengthInBits()+4<=totalDataCount*8){buffer.put(0,4);}
	while(buffer.getLengthInBits()%8!=0){buffer.putBit(false);}
	while(true){if(buffer.getLengthInBits()>=totalDataCount*8){break;}
	buffer.put(QRCodeModel.PAD0,8);if(buffer.getLengthInBits()>=totalDataCount*8){break;}
	buffer.put(QRCodeModel.PAD1,8);}
	return QRCodeModel.createBytes(buffer,rsBlocks);};QRCodeModel.createBytes=function(buffer,rsBlocks){var offset=0;var maxDcCount=0;var maxEcCount=0;var dcdata=new Array(rsBlocks.length);var ecdata=new Array(rsBlocks.length);for(var r=0;r<rsBlocks.length;r++){var dcCount=rsBlocks[r].dataCount;var ecCount=rsBlocks[r].totalCount-dcCount;maxDcCount=Math.max(maxDcCount,dcCount);maxEcCount=Math.max(maxEcCount,ecCount);dcdata[r]=new Array(dcCount);for(var i=0;i<dcdata[r].length;i++){dcdata[r][i]=0xff&buffer.buffer[i+offset];}
	offset+=dcCount;var rsPoly=QRUtil.getErrorCorrectPolynomial(ecCount);var rawPoly=new QRPolynomial(dcdata[r],rsPoly.getLength()-1);var modPoly=rawPoly.mod(rsPoly);ecdata[r]=new Array(rsPoly.getLength()-1);for(var i=0;i<ecdata[r].length;i++){var modIndex=i+modPoly.getLength()-ecdata[r].length;ecdata[r][i]=(modIndex>=0)?modPoly.get(modIndex):0;}}
	var totalCodeCount=0;for(var i=0;i<rsBlocks.length;i++){totalCodeCount+=rsBlocks[i].totalCount;}
	var data=new Array(totalCodeCount);var index=0;for(var i=0;i<maxDcCount;i++){for(var r=0;r<rsBlocks.length;r++){if(i<dcdata[r].length){data[index++]=dcdata[r][i];}}}
	for(var i=0;i<maxEcCount;i++){for(var r=0;r<rsBlocks.length;r++){if(i<ecdata[r].length){data[index++]=ecdata[r][i];}}}
	return data;};var QRMode={MODE_NUMBER:1<<0,MODE_ALPHA_NUM:1<<1,MODE_8BIT_BYTE:1<<2,MODE_KANJI:1<<3};var QRErrorCorrectLevel={L:1,M:0,Q:3,H:2};var QRMaskPattern={PATTERN000:0,PATTERN001:1,PATTERN010:2,PATTERN011:3,PATTERN100:4,PATTERN101:5,PATTERN110:6,PATTERN111:7};var QRUtil={PATTERN_POSITION_TABLE:[[],[6,18],[6,22],[6,26],[6,30],[6,34],[6,22,38],[6,24,42],[6,26,46],[6,28,50],[6,30,54],[6,32,58],[6,34,62],[6,26,46,66],[6,26,48,70],[6,26,50,74],[6,30,54,78],[6,30,56,82],[6,30,58,86],[6,34,62,90],[6,28,50,72,94],[6,26,50,74,98],[6,30,54,78,102],[6,28,54,80,106],[6,32,58,84,110],[6,30,58,86,114],[6,34,62,90,118],[6,26,50,74,98,122],[6,30,54,78,102,126],[6,26,52,78,104,130],[6,30,56,82,108,134],[6,34,60,86,112,138],[6,30,58,86,114,142],[6,34,62,90,118,146],[6,30,54,78,102,126,150],[6,24,50,76,102,128,154],[6,28,54,80,106,132,158],[6,32,58,84,110,136,162],[6,26,54,82,110,138,166],[6,30,58,86,114,142,170]],G15:(1<<10)|(1<<8)|(1<<5)|(1<<4)|(1<<2)|(1<<1)|(1<<0),G18:(1<<12)|(1<<11)|(1<<10)|(1<<9)|(1<<8)|(1<<5)|(1<<2)|(1<<0),G15_MASK:(1<<14)|(1<<12)|(1<<10)|(1<<4)|(1<<1),getBCHTypeInfo:function(data){var d=data<<10;while(QRUtil.getBCHDigit(d)-QRUtil.getBCHDigit(QRUtil.G15)>=0){d^=(QRUtil.G15<<(QRUtil.getBCHDigit(d)-QRUtil.getBCHDigit(QRUtil.G15)));}
	return((data<<10)|d)^QRUtil.G15_MASK;},getBCHTypeNumber:function(data){var d=data<<12;while(QRUtil.getBCHDigit(d)-QRUtil.getBCHDigit(QRUtil.G18)>=0){d^=(QRUtil.G18<<(QRUtil.getBCHDigit(d)-QRUtil.getBCHDigit(QRUtil.G18)));}
	return(data<<12)|d;},getBCHDigit:function(data){var digit=0;while(data!=0){digit++;data>>>=1;}
	return digit;},getPatternPosition:function(typeNumber){return QRUtil.PATTERN_POSITION_TABLE[typeNumber-1];},getMask:function(maskPattern,i,j){switch(maskPattern){case QRMaskPattern.PATTERN000:return(i+j)%2==0;case QRMaskPattern.PATTERN001:return i%2==0;case QRMaskPattern.PATTERN010:return j%3==0;case QRMaskPattern.PATTERN011:return(i+j)%3==0;case QRMaskPattern.PATTERN100:return(Math.floor(i/2)+Math.floor(j/3))%2==0;case QRMaskPattern.PATTERN101:return(i*j)%2+(i*j)%3==0;case QRMaskPattern.PATTERN110:return((i*j)%2+(i*j)%3)%2==0;case QRMaskPattern.PATTERN111:return((i*j)%3+(i+j)%2)%2==0;default:throw new Error("bad maskPattern:"+maskPattern);}},getErrorCorrectPolynomial:function(errorCorrectLength){var a=new QRPolynomial([1],0);for(var i=0;i<errorCorrectLength;i++){a=a.multiply(new QRPolynomial([1,QRMath.gexp(i)],0));}
	return a;},getLengthInBits:function(mode,type){if(1<=type&&type<10){switch(mode){case QRMode.MODE_NUMBER:return 10;case QRMode.MODE_ALPHA_NUM:return 9;case QRMode.MODE_8BIT_BYTE:return 8;case QRMode.MODE_KANJI:return 8;default:throw new Error("mode:"+mode);}}else if(type<27){switch(mode){case QRMode.MODE_NUMBER:return 12;case QRMode.MODE_ALPHA_NUM:return 11;case QRMode.MODE_8BIT_BYTE:return 16;case QRMode.MODE_KANJI:return 10;default:throw new Error("mode:"+mode);}}else if(type<41){switch(mode){case QRMode.MODE_NUMBER:return 14;case QRMode.MODE_ALPHA_NUM:return 13;case QRMode.MODE_8BIT_BYTE:return 16;case QRMode.MODE_KANJI:return 12;default:throw new Error("mode:"+mode);}}else{throw new Error("type:"+type);}},getLostPoint:function(qrCode){var moduleCount=qrCode.getModuleCount();var lostPoint=0;for(var row=0;row<moduleCount;row++){for(var col=0;col<moduleCount;col++){var sameCount=0;var dark=qrCode.isDark(row,col);for(var r=-1;r<=1;r++){if(row+r<0||moduleCount<=row+r){continue;}
	for(var c=-1;c<=1;c++){if(col+c<0||moduleCount<=col+c){continue;}
	if(r==0&&c==0){continue;}
	if(dark==qrCode.isDark(row+r,col+c)){sameCount++;}}}
	if(sameCount>5){lostPoint+=(3+sameCount-5);}}}
	for(var row=0;row<moduleCount-1;row++){for(var col=0;col<moduleCount-1;col++){var count=0;if(qrCode.isDark(row,col))count++;if(qrCode.isDark(row+1,col))count++;if(qrCode.isDark(row,col+1))count++;if(qrCode.isDark(row+1,col+1))count++;if(count==0||count==4){lostPoint+=3;}}}
	for(var row=0;row<moduleCount;row++){for(var col=0;col<moduleCount-6;col++){if(qrCode.isDark(row,col)&&!qrCode.isDark(row,col+1)&&qrCode.isDark(row,col+2)&&qrCode.isDark(row,col+3)&&qrCode.isDark(row,col+4)&&!qrCode.isDark(row,col+5)&&qrCode.isDark(row,col+6)){lostPoint+=40;}}}
	for(var col=0;col<moduleCount;col++){for(var row=0;row<moduleCount-6;row++){if(qrCode.isDark(row,col)&&!qrCode.isDark(row+1,col)&&qrCode.isDark(row+2,col)&&qrCode.isDark(row+3,col)&&qrCode.isDark(row+4,col)&&!qrCode.isDark(row+5,col)&&qrCode.isDark(row+6,col)){lostPoint+=40;}}}
	var darkCount=0;for(var col=0;col<moduleCount;col++){for(var row=0;row<moduleCount;row++){if(qrCode.isDark(row,col)){darkCount++;}}}
	var ratio=Math.abs(100*darkCount/moduleCount/moduleCount-50)/5;lostPoint+=ratio*10;return lostPoint;}};var QRMath={glog:function(n){if(n<1){throw new Error("glog("+n+")");}
	return QRMath.LOG_TABLE[n];},gexp:function(n){while(n<0){n+=255;}
	while(n>=256){n-=255;}
	return QRMath.EXP_TABLE[n];},EXP_TABLE:new Array(256),LOG_TABLE:new Array(256)};for(var i=0;i<8;i++){QRMath.EXP_TABLE[i]=1<<i;}
	for(var i=8;i<256;i++){QRMath.EXP_TABLE[i]=QRMath.EXP_TABLE[i-4]^QRMath.EXP_TABLE[i-5]^QRMath.EXP_TABLE[i-6]^QRMath.EXP_TABLE[i-8];}
	for(var i=0;i<255;i++){QRMath.LOG_TABLE[QRMath.EXP_TABLE[i]]=i;}
	function QRPolynomial(num,shift){if(num.length==undefined){throw new Error(num.length+"/"+shift);}
	var offset=0;while(offset<num.length&&num[offset]==0){offset++;}
	this.num=new Array(num.length-offset+shift);for(var i=0;i<num.length-offset;i++){this.num[i]=num[i+offset];}}
	QRPolynomial.prototype={get:function(index){return this.num[index];},getLength:function(){return this.num.length;},multiply:function(e){var num=new Array(this.getLength()+e.getLength()-1);for(var i=0;i<this.getLength();i++){for(var j=0;j<e.getLength();j++){num[i+j]^=QRMath.gexp(QRMath.glog(this.get(i))+QRMath.glog(e.get(j)));}}
	return new QRPolynomial(num,0);},mod:function(e){if(this.getLength()-e.getLength()<0){return this;}
	var ratio=QRMath.glog(this.get(0))-QRMath.glog(e.get(0));var num=new Array(this.getLength());for(var i=0;i<this.getLength();i++){num[i]=this.get(i);}
	for(var i=0;i<e.getLength();i++){num[i]^=QRMath.gexp(QRMath.glog(e.get(i))+ratio);}
	return new QRPolynomial(num,0).mod(e);}};function QRRSBlock(totalCount,dataCount){this.totalCount=totalCount;this.dataCount=dataCount;}
	QRRSBlock.RS_BLOCK_TABLE=[[1,26,19],[1,26,16],[1,26,13],[1,26,9],[1,44,34],[1,44,28],[1,44,22],[1,44,16],[1,70,55],[1,70,44],[2,35,17],[2,35,13],[1,100,80],[2,50,32],[2,50,24],[4,25,9],[1,134,108],[2,67,43],[2,33,15,2,34,16],[2,33,11,2,34,12],[2,86,68],[4,43,27],[4,43,19],[4,43,15],[2,98,78],[4,49,31],[2,32,14,4,33,15],[4,39,13,1,40,14],[2,121,97],[2,60,38,2,61,39],[4,40,18,2,41,19],[4,40,14,2,41,15],[2,146,116],[3,58,36,2,59,37],[4,36,16,4,37,17],[4,36,12,4,37,13],[2,86,68,2,87,69],[4,69,43,1,70,44],[6,43,19,2,44,20],[6,43,15,2,44,16],[4,101,81],[1,80,50,4,81,51],[4,50,22,4,51,23],[3,36,12,8,37,13],[2,116,92,2,117,93],[6,58,36,2,59,37],[4,46,20,6,47,21],[7,42,14,4,43,15],[4,133,107],[8,59,37,1,60,38],[8,44,20,4,45,21],[12,33,11,4,34,12],[3,145,115,1,146,116],[4,64,40,5,65,41],[11,36,16,5,37,17],[11,36,12,5,37,13],[5,109,87,1,110,88],[5,65,41,5,66,42],[5,54,24,7,55,25],[11,36,12],[5,122,98,1,123,99],[7,73,45,3,74,46],[15,43,19,2,44,20],[3,45,15,13,46,16],[1,135,107,5,136,108],[10,74,46,1,75,47],[1,50,22,15,51,23],[2,42,14,17,43,15],[5,150,120,1,151,121],[9,69,43,4,70,44],[17,50,22,1,51,23],[2,42,14,19,43,15],[3,141,113,4,142,114],[3,70,44,11,71,45],[17,47,21,4,48,22],[9,39,13,16,40,14],[3,135,107,5,136,108],[3,67,41,13,68,42],[15,54,24,5,55,25],[15,43,15,10,44,16],[4,144,116,4,145,117],[17,68,42],[17,50,22,6,51,23],[19,46,16,6,47,17],[2,139,111,7,140,112],[17,74,46],[7,54,24,16,55,25],[34,37,13],[4,151,121,5,152,122],[4,75,47,14,76,48],[11,54,24,14,55,25],[16,45,15,14,46,16],[6,147,117,4,148,118],[6,73,45,14,74,46],[11,54,24,16,55,25],[30,46,16,2,47,17],[8,132,106,4,133,107],[8,75,47,13,76,48],[7,54,24,22,55,25],[22,45,15,13,46,16],[10,142,114,2,143,115],[19,74,46,4,75,47],[28,50,22,6,51,23],[33,46,16,4,47,17],[8,152,122,4,153,123],[22,73,45,3,74,46],[8,53,23,26,54,24],[12,45,15,28,46,16],[3,147,117,10,148,118],[3,73,45,23,74,46],[4,54,24,31,55,25],[11,45,15,31,46,16],[7,146,116,7,147,117],[21,73,45,7,74,46],[1,53,23,37,54,24],[19,45,15,26,46,16],[5,145,115,10,146,116],[19,75,47,10,76,48],[15,54,24,25,55,25],[23,45,15,25,46,16],[13,145,115,3,146,116],[2,74,46,29,75,47],[42,54,24,1,55,25],[23,45,15,28,46,16],[17,145,115],[10,74,46,23,75,47],[10,54,24,35,55,25],[19,45,15,35,46,16],[17,145,115,1,146,116],[14,74,46,21,75,47],[29,54,24,19,55,25],[11,45,15,46,46,16],[13,145,115,6,146,116],[14,74,46,23,75,47],[44,54,24,7,55,25],[59,46,16,1,47,17],[12,151,121,7,152,122],[12,75,47,26,76,48],[39,54,24,14,55,25],[22,45,15,41,46,16],[6,151,121,14,152,122],[6,75,47,34,76,48],[46,54,24,10,55,25],[2,45,15,64,46,16],[17,152,122,4,153,123],[29,74,46,14,75,47],[49,54,24,10,55,25],[24,45,15,46,46,16],[4,152,122,18,153,123],[13,74,46,32,75,47],[48,54,24,14,55,25],[42,45,15,32,46,16],[20,147,117,4,148,118],[40,75,47,7,76,48],[43,54,24,22,55,25],[10,45,15,67,46,16],[19,148,118,6,149,119],[18,75,47,31,76,48],[34,54,24,34,55,25],[20,45,15,61,46,16]];QRRSBlock.getRSBlocks=function(typeNumber,errorCorrectLevel){var rsBlock=QRRSBlock.getRsBlockTable(typeNumber,errorCorrectLevel);if(rsBlock==undefined){throw new Error("bad rs block @ typeNumber:"+typeNumber+"/errorCorrectLevel:"+errorCorrectLevel);}
	var length=rsBlock.length/3;var list=[];for(var i=0;i<length;i++){var count=rsBlock[i*3+0];var totalCount=rsBlock[i*3+1];var dataCount=rsBlock[i*3+2];for(var j=0;j<count;j++){list.push(new QRRSBlock(totalCount,dataCount));}}
	return list;};QRRSBlock.getRsBlockTable=function(typeNumber,errorCorrectLevel){switch(errorCorrectLevel){case QRErrorCorrectLevel.L:return QRRSBlock.RS_BLOCK_TABLE[(typeNumber-1)*4+0];case QRErrorCorrectLevel.M:return QRRSBlock.RS_BLOCK_TABLE[(typeNumber-1)*4+1];case QRErrorCorrectLevel.Q:return QRRSBlock.RS_BLOCK_TABLE[(typeNumber-1)*4+2];case QRErrorCorrectLevel.H:return QRRSBlock.RS_BLOCK_TABLE[(typeNumber-1)*4+3];default:return undefined;}};function QRBitBuffer(){this.buffer=[];this.length=0;}
	QRBitBuffer.prototype={get:function(index){var bufIndex=Math.floor(index/8);return((this.buffer[bufIndex]>>>(7-index%8))&1)==1;},put:function(num,length){for(var i=0;i<length;i++){this.putBit(((num>>>(length-i-1))&1)==1);}},getLengthInBits:function(){return this.length;},putBit:function(bit){var bufIndex=Math.floor(this.length/8);if(this.buffer.length<=bufIndex){this.buffer.push(0);}
	if(bit){this.buffer[bufIndex]|=(0x80>>>(this.length%8));}
	this.length++;}};var QRCodeLimitLength=[[17,14,11,7],[32,26,20,14],[53,42,32,24],[78,62,46,34],[106,84,60,44],[134,106,74,58],[154,122,86,64],[192,152,108,84],[230,180,130,98],[271,213,151,119],[321,251,177,137],[367,287,203,155],[425,331,241,177],[458,362,258,194],[520,412,292,220],[586,450,322,250],[644,504,364,280],[718,560,394,310],[792,624,442,338],[858,666,482,382],[929,711,509,403],[1003,779,565,439],[1091,857,611,461],[1171,911,661,511],[1273,997,715,535],[1367,1059,751,593],[1465,1125,805,625],[1528,1190,868,658],[1628,1264,908,698],[1732,1370,982,742],[1840,1452,1030,790],[1952,1538,1112,842],[2068,1628,1168,898],[2188,1722,1228,958],[2303,1809,1283,983],[2431,1911,1351,1051],[2563,1989,1423,1093],[2699,2099,1499,1139],[2809,2213,1579,1219],[2953,2331,1663,1273]];
	
	function _isSupportCanvas() {
		return typeof CanvasRenderingContext2D != "undefined";
	}
	
	// android 2.x doesn't support Data-URI spec
	function _getAndroid() {
		var android = false;
		var sAgent = navigator.userAgent;
		
		if (/android/i.test(sAgent)) { // android
			android = true;
			var aMat = sAgent.toString().match(/android ([0-9]\.[0-9])/i);
			
			if (aMat && aMat[1]) {
				android = parseFloat(aMat[1]);
			}
		}
		
		return android;
	}
	
	var svgDrawer = (function() {

		var Drawing = function (el, htOption) {
			this._el = el;
			this._htOption = htOption;
		};

		Drawing.prototype.draw = function (oQRCode) {
			var _htOption = this._htOption;
			var _el = this._el;
			var nCount = oQRCode.getModuleCount();
			var nWidth = Math.floor(_htOption.width / nCount);
			var nHeight = Math.floor(_htOption.height / nCount);

			this.clear();

			function makeSVG(tag, attrs) {
				var el = document.createElementNS('http://www.w3.org/2000/svg', tag);
				for (var k in attrs)
					if (attrs.hasOwnProperty(k)) el.setAttribute(k, attrs[k]);
				return el;
			}

			var svg = makeSVG("svg" , {'viewBox': '0 0 ' + String(nCount) + " " + String(nCount), 'width': '100%', 'height': '100%', 'fill': _htOption.colorLight});
			svg.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xlink", "http://www.w3.org/1999/xlink");
			_el.appendChild(svg);

			svg.appendChild(makeSVG("rect", {"fill": _htOption.colorLight, "width": "100%", "height": "100%"}));
			svg.appendChild(makeSVG("rect", {"fill": _htOption.colorDark, "width": "1", "height": "1", "id": "template"}));

			for (var row = 0; row < nCount; row++) {
				for (var col = 0; col < nCount; col++) {
					if (oQRCode.isDark(row, col)) {
						var child = makeSVG("use", {"x": String(row), "y": String(col)});
						child.setAttributeNS("http://www.w3.org/1999/xlink", "href", "#template")
						svg.appendChild(child);
					}
				}
			}
		};
		Drawing.prototype.clear = function () {
			while (this._el.hasChildNodes())
				this._el.removeChild(this._el.lastChild);
		};
		return Drawing;
	})();

	var useSVG = document.documentElement.tagName.toLowerCase() === "svg";

	// Drawing in DOM by using Table tag
	var Drawing = useSVG ? svgDrawer : !_isSupportCanvas() ? (function () {
		var Drawing = function (el, htOption) {
			this._el = el;
			this._htOption = htOption;
		};
			
		/**
		 * Draw the QRCode
		 * 
		 * @param {QRCode} oQRCode
		 */
		Drawing.prototype.draw = function (oQRCode) {
            var _htOption = this._htOption;
            var _el = this._el;
			var nCount = oQRCode.getModuleCount();
			var nWidth = Math.floor(_htOption.width / nCount);
			var nHeight = Math.floor(_htOption.height / nCount);
			var aHTML = ['<table style="border:0;border-collapse:collapse;">'];
			
			for (var row = 0; row < nCount; row++) {
				aHTML.push('<tr>');
				
				for (var col = 0; col < nCount; col++) {
					aHTML.push('<td style="border:0;border-collapse:collapse;padding:0;margin:0;width:' + nWidth + 'px;height:' + nHeight + 'px;background-color:' + (oQRCode.isDark(row, col) ? _htOption.colorDark : _htOption.colorLight) + ';"></td>');
				}
				
				aHTML.push('</tr>');
			}
			
			aHTML.push('</table>');
			_el.innerHTML = aHTML.join('');
			
			// Fix the margin values as real size.
			var elTable = _el.childNodes[0];
			var nLeftMarginTable = (_htOption.width - elTable.offsetWidth) / 2;
			var nTopMarginTable = (_htOption.height - elTable.offsetHeight) / 2;
			
			if (nLeftMarginTable > 0 && nTopMarginTable > 0) {
				elTable.style.margin = nTopMarginTable + "px " + nLeftMarginTable + "px";	
			}
		};
		
		/**
		 * Clear the QRCode
		 */
		Drawing.prototype.clear = function () {
			this._el.innerHTML = '';
		};
		
		return Drawing;
	})() : (function () { // Drawing in Canvas
		function _onMakeImage() {
			this._elImage.src = this._elCanvas.toDataURL("image/png");
			this._elImage.style.display = "block";
			this._elCanvas.style.display = "none";			
		}
		
		// Android 2.1 bug workaround
		// http://code.google.com/p/android/issues/detail?id=5141
		if (this._android && this._android <= 2.1) {
	    	var factor = 1 / window.devicePixelRatio;
	        var drawImage = CanvasRenderingContext2D.prototype.drawImage; 
	    	CanvasRenderingContext2D.prototype.drawImage = function (image, sx, sy, sw, sh, dx, dy, dw, dh) {
	    		if (("nodeName" in image) && /img/i.test(image.nodeName)) {
		        	for (var i = arguments.length - 1; i >= 1; i--) {
		            	arguments[i] = arguments[i] * factor;
		        	}
	    		} else if (typeof dw == "undefined") {
	    			arguments[1] *= factor;
	    			arguments[2] *= factor;
	    			arguments[3] *= factor;
	    			arguments[4] *= factor;
	    		}
	    		
	        	drawImage.apply(this, arguments); 
	    	};
		}
		
		/**
		 * Check whether the user's browser supports Data URI or not
		 * 
		 * @private
		 * @param {Function} fSuccess Occurs if it supports Data URI
		 * @param {Function} fFail Occurs if it doesn't support Data URI
		 */
		function _safeSetDataURI(fSuccess, fFail) {
            var self = this;
            self._fFail = fFail;
            self._fSuccess = fSuccess;

            // Check it just once
            if (self._bSupportDataURI === null) {
                var el = document.createElement("img");
                var fOnError = function() {
                    self._bSupportDataURI = false;

                    if (self._fFail) {
                        self._fFail.call(self);
                    }
                };
                var fOnSuccess = function() {
                    self._bSupportDataURI = true;

                    if (self._fSuccess) {
                        self._fSuccess.call(self);
                    }
                };

                el.onabort = fOnError;
                el.onerror = fOnError;
                el.onload = fOnSuccess;
                el.src = "data:image/gif;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg=="; // the Image contains 1px data.
                return;
            } else if (self._bSupportDataURI === true && self._fSuccess) {
                self._fSuccess.call(self);
            } else if (self._bSupportDataURI === false && self._fFail) {
                self._fFail.call(self);
            }
		};
		
		/**
		 * Drawing QRCode by using canvas
		 * 
		 * @constructor
		 * @param {HTMLElement} el
		 * @param {Object} htOption QRCode Options 
		 */
		var Drawing = function (el, htOption) {
    		this._bIsPainted = false;
    		this._android = _getAndroid();
		
			this._htOption = htOption;
			this._elCanvas = document.createElement("canvas");
			this._elCanvas.width = htOption.width;
			this._elCanvas.height = htOption.height;
			el.appendChild(this._elCanvas);
			this._el = el;
			this._oContext = this._elCanvas.getContext("2d");
			this._bIsPainted = false;
			this._elImage = document.createElement("img");
			this._elImage.alt = "Scan me!";
			this._elImage.style.display = "none";
			this._el.appendChild(this._elImage);
			this._bSupportDataURI = null;
		};
			
		/**
		 * Draw the QRCode
		 * 
		 * @param {QRCode} oQRCode 
		 */
		Drawing.prototype.draw = function (oQRCode) {
            var _elImage = this._elImage;
            var _oContext = this._oContext;
            var _htOption = this._htOption;
            
			var nCount = oQRCode.getModuleCount();
			var nWidth = _htOption.width / nCount;
			var nHeight = _htOption.height / nCount;
			var nRoundedWidth = Math.round(nWidth);
			var nRoundedHeight = Math.round(nHeight);

			_elImage.style.display = "none";
			this.clear();
			
			for (var row = 0; row < nCount; row++) {
				for (var col = 0; col < nCount; col++) {
					var bIsDark = oQRCode.isDark(row, col);
					var nLeft = col * nWidth;
					var nTop = row * nHeight;
					_oContext.strokeStyle = bIsDark ? _htOption.colorDark : _htOption.colorLight;
					_oContext.lineWidth = 1;
					_oContext.fillStyle = bIsDark ? _htOption.colorDark : _htOption.colorLight;					
					_oContext.fillRect(nLeft, nTop, nWidth, nHeight);
					
					// 안티 앨리어싱 방지 처리
					_oContext.strokeRect(
						Math.floor(nLeft) + 0.5,
						Math.floor(nTop) + 0.5,
						nRoundedWidth,
						nRoundedHeight
					);
					
					_oContext.strokeRect(
						Math.ceil(nLeft) - 0.5,
						Math.ceil(nTop) - 0.5,
						nRoundedWidth,
						nRoundedHeight
					);
				}
			}
			
			this._bIsPainted = true;
		};
			
		/**
		 * Make the image from Canvas if the browser supports Data URI.
		 */
		Drawing.prototype.makeImage = function () {
			if (this._bIsPainted) {
				_safeSetDataURI.call(this, _onMakeImage);
			}
		};
			
		/**
		 * Return whether the QRCode is painted or not
		 * 
		 * @return {Boolean}
		 */
		Drawing.prototype.isPainted = function () {
			return this._bIsPainted;
		};
		
		/**
		 * Clear the QRCode
		 */
		Drawing.prototype.clear = function () {
			this._oContext.clearRect(0, 0, this._elCanvas.width, this._elCanvas.height);
			this._bIsPainted = false;
		};
		
		/**
		 * @private
		 * @param {Number} nNumber
		 */
		Drawing.prototype.round = function (nNumber) {
			if (!nNumber) {
				return nNumber;
			}
			
			return Math.floor(nNumber * 1000) / 1000;
		};
		
		return Drawing;
	})();
	
	/**
	 * Get the type by string length
	 * 
	 * @private
	 * @param {String} sText
	 * @param {Number} nCorrectLevel
	 * @return {Number} type
	 */
	function _getTypeNumber(sText, nCorrectLevel) {			
		var nType = 1;
		var length = _getUTF8Length(sText);
		
		for (var i = 0, len = QRCodeLimitLength.length; i <= len; i++) {
			var nLimit = 0;
			
			switch (nCorrectLevel) {
				case QRErrorCorrectLevel.L :
					nLimit = QRCodeLimitLength[i][0];
					break;
				case QRErrorCorrectLevel.M :
					nLimit = QRCodeLimitLength[i][1];
					break;
				case QRErrorCorrectLevel.Q :
					nLimit = QRCodeLimitLength[i][2];
					break;
				case QRErrorCorrectLevel.H :
					nLimit = QRCodeLimitLength[i][3];
					break;
			}
			
			if (length <= nLimit) {
				break;
			} else {
				nType++;
			}
		}
		
		if (nType > QRCodeLimitLength.length) {
			throw new Error("Too long data");
		}
		
		return nType;
	}

	function _getUTF8Length(sText) {
		var replacedText = encodeURI(sText).toString().replace(/\%[0-9a-fA-F]{2}/g, 'a');
		return replacedText.length + (replacedText.length != sText ? 3 : 0);
	}
	
	/**
	 * @class QRCode
	 * @constructor
	 * @example 
	 * new QRCode(document.getElementById("test"), "http://jindo.dev.naver.com/collie");
	 *
	 * @example
	 * var oQRCode = new QRCode("test", {
	 *    text : "http://naver.com",
	 *    width : 128,
	 *    height : 128
	 * });
	 * 
	 * oQRCode.clear(); // Clear the QRCode.
	 * oQRCode.makeCode("http://map.naver.com"); // Re-create the QRCode.
	 *
	 * @param {HTMLElement|String} el target element or 'id' attribute of element.
	 * @param {Object|String} vOption
	 * @param {String} vOption.text QRCode link data
	 * @param {Number} [vOption.width=256]
	 * @param {Number} [vOption.height=256]
	 * @param {String} [vOption.colorDark="#000000"]
	 * @param {String} [vOption.colorLight="#ffffff"]
	 * @param {QRCode.CorrectLevel} [vOption.correctLevel=QRCode.CorrectLevel.H] [L|M|Q|H] 
	 */
	QRCode = function (el, vOption) {
		this._htOption = {
			width : 256, 
			height : 256,
			typeNumber : 4,
			colorDark : "#000000",
			colorLight : "#ffffff",
			correctLevel : QRErrorCorrectLevel.H
		};
		
		if (typeof vOption === 'string') {
			vOption	= {
				text : vOption
			};
		}
		
		// Overwrites options
		if (vOption) {
			for (var i in vOption) {
				this._htOption[i] = vOption[i];
			}
		}
		
		if (typeof el == "string") {
			el = document.getElementById(el);
		}

		if (this._htOption.useSVG) {
			Drawing = svgDrawer;
		}
		
		this._android = _getAndroid();
		this._el = el;
		this._oQRCode = null;
		this._oDrawing = new Drawing(this._el, this._htOption);
		
		if (this._htOption.text) {
			this.makeCode(this._htOption.text);	
		}
	};
	
	/**
	 * Make the QRCode
	 * 
	 * @param {String} sText link data
	 */
	QRCode.prototype.makeCode = function (sText) {
		this._oQRCode = new QRCodeModel(_getTypeNumber(sText, this._htOption.correctLevel), this._htOption.correctLevel);
		this._oQRCode.addData(sText);
		this._oQRCode.make();
		this._el.title = sText;
		this._oDrawing.draw(this._oQRCode);			
		this.makeImage();
	};
	
	/**
	 * Make the Image from Canvas element
	 * - It occurs automatically
	 * - Android below 3 doesn't support Data-URI spec.
	 * 
	 * @private
	 */
	QRCode.prototype.makeImage = function () {
		if (typeof this._oDrawing.makeImage == "function" && (!this._android || this._android >= 3)) {
			this._oDrawing.makeImage();
		}
	};
	
	/**
	 * Clear the QRCode
	 */
	QRCode.prototype.clear = function () {
		this._oDrawing.clear();
	};
	
	/**
	 * @name QRCode.CorrectLevel
	 */
	QRCode.CorrectLevel = QRErrorCorrectLevel;
})();

/*
 Highstock JS v7.2.1 (2019-10-31)

 (c) 2009-2018 Torstein Honsi

 License: www.highcharts.com/license
*/
(function(R,K){"object"===typeof module&&module.exports?(K["default"]=K,module.exports=R.document?K(R):K):"function"===typeof define&&define.amd?define("highcharts/highstock",function(){return K(R)}):(R.Highcharts&&R.Highcharts.error(16,!0),R.Highcharts=K(R))})("undefined"!==typeof window?window:this,function(R){function K(c,g,I,G){c.hasOwnProperty(g)||(c[g]=G.apply(null,I))}var D={};K(D,"parts/Globals.js",[],function(){var c="undefined"!==typeof R?R:"undefined"!==typeof window?window:{},g=c.document,
I=c.navigator&&c.navigator.userAgent||"",G=g&&g.createElementNS&&!!g.createElementNS("http://www.w3.org/2000/svg","svg").createSVGRect,H=/(edge|msie|trident)/i.test(I)&&!c.opera,y=-1!==I.indexOf("Firefox"),w=-1!==I.indexOf("Chrome"),x=y&&4>parseInt(I.split("Firefox/")[1],10);return{product:"Highcharts",version:"7.2.1",deg2rad:2*Math.PI/360,doc:g,hasBidiBug:x,hasTouch:!!c.TouchEvent,isMS:H,isWebKit:-1!==I.indexOf("AppleWebKit"),isFirefox:y,isChrome:w,isSafari:!w&&-1!==I.indexOf("Safari"),isTouchDevice:/(Mobile|Android|Windows Phone)/.test(I),
SVG_NS:"http://www.w3.org/2000/svg",chartCount:0,seriesTypes:{},symbolSizes:{},svg:G,win:c,marginNames:["plotTop","marginRight","marginBottom","plotLeft"],noop:function(){},charts:[],dateFormats:{}}});K(D,"parts/Utilities.js",[D["parts/Globals.js"]],function(c){function g(b,a){return parseInt(b,a||10)}function I(b){return"string"===typeof b}function G(b){b=Object.prototype.toString.call(b);return"[object Array]"===b||"[object Array Iterator]"===b}function H(b,a){return!!b&&"object"===typeof b&&(!a||
!G(b))}function y(b){return H(b)&&"number"===typeof b.nodeType}function w(b){var a=b&&b.constructor;return!(!H(b,!0)||y(b)||!a||!a.name||"Object"===a.name)}function x(b){return"number"===typeof b&&!isNaN(b)&&Infinity>b&&-Infinity<b}function E(b){return"undefined"!==typeof b&&null!==b}function F(b,a,e){var d;I(a)?E(e)?b.setAttribute(a,e):b&&b.getAttribute&&((d=b.getAttribute(a))||"class"!==a||(d=b.getAttribute(a+"Name"))):p(a,function(a,d){b.setAttribute(d,a)});return d}function t(b,a){var d;b||(b=
{});for(d in a)b[d]=a[d];return b}function m(){for(var b=arguments,a=b.length,e=0;e<a;e++){var k=b[e];if("undefined"!==typeof k&&null!==k)return k}}function p(b,a,e){for(var d in b)Object.hasOwnProperty.call(b,d)&&a.call(e||b[d],b[d],d,b)}c.timers=[];var q=c.charts,h=c.doc,a=c.win;c.error=function(b,d,e,k){var C=x(b),h=C?"Highcharts error #"+b+": www.highcharts.com/errors/"+b+"/":b.toString(),r=function(){if(d)throw Error(h);a.console&&console.log(h)};if("undefined"!==typeof k){var n="";C&&(h+="?");
c.objectEach(k,function(b,a){n+="\n"+a+": "+b;C&&(h+=encodeURI(a)+"="+encodeURI(b))});h+=n}e?c.fireEvent(e,"displayError",{code:b,message:h,params:k},r):r()};c.Fx=function(b,a,e){this.options=a;this.elem=b;this.prop=e};c.Fx.prototype={dSetter:function(){var b=this.paths[0],a=this.paths[1],e=[],k=this.now,C=b.length;if(1===k)e=this.toD;else if(C===a.length&&1>k)for(;C--;){var h=parseFloat(b[C]);e[C]=isNaN(h)||"A"===a[C-4]||"A"===a[C-5]?a[C]:k*parseFloat(""+(a[C]-h))+h}else e=a;this.elem.attr("d",e,
null,!0)},update:function(){var b=this.elem,a=this.prop,e=this.now,k=this.options.step;if(this[a+"Setter"])this[a+"Setter"]();else b.attr?b.element&&b.attr(a,e,null,!0):b.style[a]=e+this.unit;k&&k.call(b,e,this)},run:function(b,d,e){var k=this,C=k.options,h=function(b){return h.stopped?!1:k.step(b)},r=a.requestAnimationFrame||function(b){setTimeout(b,13)},n=function(){for(var b=0;b<c.timers.length;b++)c.timers[b]()||c.timers.splice(b--,1);c.timers.length&&r(n)};b!==d||this.elem["forceAnimate:"+this.prop]?
(this.startTime=+new Date,this.start=b,this.end=d,this.unit=e,this.now=this.start,this.pos=0,h.elem=this.elem,h.prop=this.prop,h()&&1===c.timers.push(h)&&r(n)):(delete C.curAnim[this.prop],C.complete&&0===Object.keys(C.curAnim).length&&C.complete.call(this.elem))},step:function(b){var a=+new Date,e=this.options,k=this.elem,h=e.complete,c=e.duration,r=e.curAnim;if(k.attr&&!k.element)b=!1;else if(b||a>=c+this.startTime){this.now=this.end;this.pos=1;this.update();var n=r[this.prop]=!0;p(r,function(b){!0!==
b&&(n=!1)});n&&h&&h.call(k);b=!1}else this.pos=e.easing((a-this.startTime)/c),this.now=this.start+(this.end-this.start)*this.pos,this.update(),b=!0;return b},initPath:function(b,a,e){function d(b){for(B=b.length;B--;){var a="M"===b[B]||"L"===b[B];var l=/[a-zA-Z]/.test(b[B+3]);a&&l&&b.splice(B+1,0,b[B+1],b[B+2],b[B+1],b[B+2])}}function h(b,a){for(;b.length<m;){b[0]=a[m-b.length];var f=b.slice(0,l);[].splice.apply(b,[0,0].concat(f));A&&(f=b.slice(b.length-l),[].splice.apply(b,[b.length,0].concat(f)),
B--)}b[0]="M"}function c(b,a){for(var d=(m-b.length)/l;0<d&&d--;)v=b.slice().splice(b.length/u-l,l*u),v[0]=a[m-l-d*l],f&&(v[l-6]=v[l-2],v[l-5]=v[l-1]),[].splice.apply(b,[b.length/u,0].concat(v)),A&&d--}a=a||"";var r=b.startX,n=b.endX,f=-1<a.indexOf("C"),l=f?7:3,v,B;a=a.split(" ");e=e.slice();var A=b.isArea,u=A?2:1;f&&(d(a),d(e));if(r&&n){for(B=0;B<r.length;B++)if(r[B]===n[0]){var J=B;break}else if(r[0]===n[n.length-r.length+B]){J=B;var L=!0;break}else if(r[r.length-1]===n[n.length-r.length+B]){J=
r.length-B;break}"undefined"===typeof J&&(a=[])}if(a.length&&x(J)){var m=e.length+J*u*l;L?(h(a,e),c(e,a)):(h(e,a),c(a,e))}return[a,e]},fillSetter:function(){c.Fx.prototype.strokeSetter.apply(this,arguments)},strokeSetter:function(){this.elem.attr(this.prop,c.color(this.start).tweenTo(c.color(this.end),this.pos),null,!0)}};c.merge=function(){var b,a=arguments,e={},k=function(b,a){"object"!==typeof b&&(b={});p(a,function(d,f){!H(d,!0)||w(d)||y(d)?b[f]=a[f]:b[f]=k(b[f]||{},d)});return b};!0===a[0]&&
(e=a[1],a=Array.prototype.slice.call(a,2));var h=a.length;for(b=0;b<h;b++)e=k(e,a[b]);return e};c.clearTimeout=function(b){E(b)&&clearTimeout(b)};c.css=function(b,a){c.isMS&&!c.svg&&a&&"undefined"!==typeof a.opacity&&(a.filter="alpha(opacity="+100*a.opacity+")");t(b.style,a)};c.createElement=function(b,a,e,k,C){b=h.createElement(b);var d=c.css;a&&t(b,a);C&&d(b,{padding:"0",border:"none",margin:"0"});e&&d(b,e);k&&k.appendChild(b);return b};c.extendClass=function(b,a){var d=function(){};d.prototype=
new b;t(d.prototype,a);return d};c.pad=function(b,a,e){return Array((a||2)+1-String(b).replace("-","").length).join(e||"0")+b};c.relativeLength=function(b,a,e){return/%$/.test(b)?a*parseFloat(b)/100+(e||0):parseFloat(b)};c.wrap=function(b,a,e){var d=b[a];b[a]=function(){var b=Array.prototype.slice.call(arguments),a=arguments,k=this;k.proceed=function(){d.apply(k,arguments.length?arguments:a)};b.unshift(d);b=e.apply(this,b);k.proceed=null;return b}};c.datePropsToTimestamps=function(b){p(b,function(a,
e){H(a)&&"function"===typeof a.getTime?b[e]=a.getTime():(H(a)||G(a))&&c.datePropsToTimestamps(a)})};c.formatSingle=function(b,a,e){var d=/\.([0-9])/,h=c.defaultOptions.lang;/f$/.test(b)?(e=(e=b.match(d))?e[1]:-1,null!==a&&(a=c.numberFormat(a,e,h.decimalPoint,-1<b.indexOf(",")?h.thousandsSep:""))):a=(e||c.time).dateFormat(b,a);return a};c.format=function(b,a,e){for(var d="{",h=!1,m,r,n,f,l=[],v;b;){d=b.indexOf(d);if(-1===d)break;m=b.slice(0,d);if(h){m=m.split(":");r=m.shift().split(".");f=r.length;
v=a;for(n=0;n<f;n++)v&&(v=v[r[n]]);m.length&&(v=c.formatSingle(m.join(":"),v,e));l.push(v)}else l.push(m);b=b.slice(d+1);d=(h=!h)?"}":"{"}l.push(b);return l.join("")};c.getMagnitude=function(b){return Math.pow(10,Math.floor(Math.log(b)/Math.LN10))};c.normalizeTickInterval=function(b,a,e,k,h){var d=b;e=m(e,1);var r=b/e;a||(a=h?[1,1.2,1.5,2,2.5,3,4,5,6,8,10]:[1,2,2.5,5,10],!1===k&&(1===e?a=a.filter(function(b){return 0===b%1}):.1>=e&&(a=[1/e])));for(k=0;k<a.length&&!(d=a[k],h&&d*e>=b||!h&&r<=(a[k]+
(a[k+1]||a[k]))/2);k++);return d=c.correctFloat(d*e,-Math.round(Math.log(.001)/Math.LN10))};c.stableSort=function(b,a){var d=b.length,k,h;for(h=0;h<d;h++)b[h].safeI=h;b.sort(function(b,d){k=a(b,d);return 0===k?b.safeI-d.safeI:k});for(h=0;h<d;h++)delete b[h].safeI};c.correctFloat=function(b,a){return parseFloat(b.toPrecision(a||14))};c.animObject=function(b){return H(b)?c.merge(b):{duration:b?500:0}};c.timeUnits={millisecond:1,second:1E3,minute:6E4,hour:36E5,day:864E5,week:6048E5,month:24192E5,year:314496E5};
c.numberFormat=function(b,a,e,k){b=+b||0;a=+a;var d=c.defaultOptions.lang,h=(b.toString().split(".")[1]||"").split("e")[0].length,r=b.toString().split("e");if(-1===a)a=Math.min(h,20);else if(!x(a))a=2;else if(a&&r[1]&&0>r[1]){var n=a+ +r[1];0<=n?(r[0]=(+r[0]).toExponential(n).split("e")[0],a=n):(r[0]=r[0].split(".")[0]||0,b=20>a?(r[0]*Math.pow(10,r[1])).toFixed(a):0,r[1]=0)}var f=(Math.abs(r[1]?r[0]:b)+Math.pow(10,-Math.max(a,h)-1)).toFixed(a);h=String(g(f));n=3<h.length?h.length%3:0;e=m(e,d.decimalPoint);
k=m(k,d.thousandsSep);b=(0>b?"-":"")+(n?h.substr(0,n)+k:"");b+=h.substr(n).replace(/(\d{3})(?=\d)/g,"$1"+k);a&&(b+=e+f.slice(-a));r[1]&&0!==+b&&(b+="e"+r[1]);return b};Math.easeInOutSine=function(a){return-.5*(Math.cos(Math.PI*a)-1)};c.getStyle=function(b,d,e){if("width"===d)return d=Math.min(b.offsetWidth,b.scrollWidth),e=b.getBoundingClientRect&&b.getBoundingClientRect().width,e<d&&e>=d-1&&(d=Math.floor(e)),Math.max(0,d-c.getStyle(b,"padding-left")-c.getStyle(b,"padding-right"));if("height"===d)return Math.max(0,
Math.min(b.offsetHeight,b.scrollHeight)-c.getStyle(b,"padding-top")-c.getStyle(b,"padding-bottom"));a.getComputedStyle||c.error(27,!0);if(b=a.getComputedStyle(b,void 0))b=b.getPropertyValue(d),m(e,"opacity"!==d)&&(b=g(b));return b};c.inArray=function(a,d,e){return d.indexOf(a,e)};c.find=Array.prototype.find?function(a,d){return a.find(d)}:function(a,d){var b,k=a.length;for(b=0;b<k;b++)if(d(a[b],b))return a[b]};c.keys=Object.keys;c.offset=function(b){var d=h.documentElement;b=b.parentElement||b.parentNode?
b.getBoundingClientRect():{top:0,left:0};return{top:b.top+(a.pageYOffset||d.scrollTop)-(d.clientTop||0),left:b.left+(a.pageXOffset||d.scrollLeft)-(d.clientLeft||0)}};c.stop=function(a,d){for(var b=c.timers.length;b--;)c.timers[b].elem!==a||d&&d!==c.timers[b].prop||(c.timers[b].stopped=!0)};p({map:"map",each:"forEach",grep:"filter",reduce:"reduce",some:"some"},function(a,d){c[d]=function(b){return Array.prototype[a].apply(b,[].slice.call(arguments,1))}});c.addEvent=function(a,d,e,k){void 0===k&&(k=
{});var b=a.addEventListener||c.addEventListenerPolyfill;var h="function"===typeof a&&a.prototype?a.prototype.protoEvents=a.prototype.protoEvents||{}:a.hcEvents=a.hcEvents||{};c.Point&&a instanceof c.Point&&a.series&&a.series.chart&&(a.series.chart.runTrackerClick=!0);b&&b.call(a,d,e,!1);h[d]||(h[d]=[]);h[d].push({fn:e,order:"number"===typeof k.order?k.order:Infinity});h[d].sort(function(a,b){return a.order-b.order});return function(){c.removeEvent(a,d,e)}};c.removeEvent=function(a,d,e){function b(b,
d){var f=a.removeEventListener||c.removeEventListenerPolyfill;f&&f.call(a,b,d,!1)}function h(e){var k;if(a.nodeName){if(d){var f={};f[d]=!0}else f=e;p(f,function(a,f){if(e[f])for(k=e[f].length;k--;)b(f,e[f][k].fn)})}}var m;["protoEvents","hcEvents"].forEach(function(k,n){var f=(n=n?a:a.prototype)&&n[k];f&&(d?(m=f[d]||[],e?(f[d]=m.filter(function(a){return e!==a.fn}),b(d,e)):(h(f),f[d]=[])):(h(f),n[k]={}))})};c.fireEvent=function(a,d,e,k){var b;e=e||{};if(h.createEvent&&(a.dispatchEvent||a.fireEvent)){var c=
h.createEvent("Events");c.initEvent(d,!0,!0);t(c,e);a.dispatchEvent?a.dispatchEvent(c):a.fireEvent(d,c)}else e.target||t(e,{preventDefault:function(){e.defaultPrevented=!0},target:a,type:d}),function(d,k){void 0===d&&(d=[]);void 0===k&&(k=[]);var f=0,l=0,n=d.length+k.length;for(b=0;b<n;b++)!1===(d[f]?k[l]?d[f].order<=k[l].order?d[f++]:k[l++]:d[f++]:k[l++]).fn.call(a,e)&&e.preventDefault()}(a.protoEvents&&a.protoEvents[d],a.hcEvents&&a.hcEvents[d]);k&&!e.defaultPrevented&&k.call(a,e)};c.animate=function(a,
d,e){var b,h="",m,r;if(!H(e)){var n=arguments;e={duration:n[2],easing:n[3],complete:n[4]}}x(e.duration)||(e.duration=400);e.easing="function"===typeof e.easing?e.easing:Math[e.easing]||Math.easeInOutSine;e.curAnim=c.merge(d);p(d,function(f,l){c.stop(a,l);r=new c.Fx(a,e,l);m=null;"d"===l?(r.paths=r.initPath(a,a.d,d.d),r.toD=d.d,b=0,m=1):a.attr?b=a.attr(l):(b=parseFloat(c.getStyle(a,l))||0,"opacity"!==l&&(h="px"));m||(m=f);m&&m.match&&m.match("px")&&(m=m.replace(/px/g,""));r.run(b,m,h)})};c.seriesType=
function(a,d,e,k,h){var b=c.getOptions(),r=c.seriesTypes;b.plotOptions[a]=c.merge(b.plotOptions[d],e);r[a]=c.extendClass(r[d]||function(){},k);r[a].prototype.type=a;h&&(r[a].prototype.pointClass=c.extendClass(c.Point,h));return r[a]};c.uniqueKey=function(){var a=Math.random().toString(36).substring(2,9),d=0;return function(){return"highcharts-"+a+"-"+d++}}();c.isFunction=function(a){return"function"===typeof a};a.jQuery&&(a.jQuery.fn.highcharts=function(){var a=[].slice.call(arguments);if(this[0])return a[0]?
(new (c[I(a[0])?a.shift():"Chart"])(this[0],a[0],a[1]),this):q[F(this[0],"data-highcharts-chart")]});return{arrayMax:function(a){for(var b=a.length,e=a[0];b--;)a[b]>e&&(e=a[b]);return e},arrayMin:function(a){for(var b=a.length,e=a[0];b--;)a[b]<e&&(e=a[b]);return e},attr:F,defined:E,destroyObjectProperties:function(a,d){p(a,function(b,k){b&&b!==d&&b.destroy&&b.destroy();delete a[k]})},discardElement:function(a){var b=c.garbageBin;b||(b=c.createElement("div"));a&&b.appendChild(a);b.innerHTML=""},erase:function(a,
d){for(var b=a.length;b--;)if(a[b]===d){a.splice(b,1);break}},extend:t,isArray:G,isClass:w,isDOMElement:y,isNumber:x,isObject:H,isString:I,objectEach:p,pick:m,pInt:g,setAnimation:function(a,d){d.renderer.globalAnimation=m(a,d.options.chart.animation,!0)},splat:function(a){return G(a)?a:[a]},syncTimeout:function(a,d,e){if(0<d)return setTimeout(a,d,e);a.call(0,e);return-1}}});K(D,"parts/Color.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.isNumber,G=g.pInt,H=c.merge;c.Color=
function(g){if(!(this instanceof c.Color))return new c.Color(g);this.init(g)};c.Color.prototype={parsers:[{regex:/rgba\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]?(?:\.[0-9]+)?)\s*\)/,parse:function(c){return[G(c[1]),G(c[2]),G(c[3]),parseFloat(c[4],10)]}},{regex:/rgb\(\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*,\s*([0-9]{1,3})\s*\)/,parse:function(c){return[G(c[1]),G(c[2]),G(c[3]),1]}}],names:{white:"#ffffff",black:"#000000"},init:function(g){var w,x;if((this.input=g=this.names[g&&
g.toLowerCase?g.toLowerCase():""]||g)&&g.stops)this.stops=g.stops.map(function(g){return new c.Color(g[1])});else{if(g&&g.charAt&&"#"===g.charAt()){var E=g.length;g=parseInt(g.substr(1),16);7===E?w=[(g&16711680)>>16,(g&65280)>>8,g&255,1]:4===E&&(w=[(g&3840)>>4|(g&3840)>>8,(g&240)>>4|g&240,(g&15)<<4|g&15,1])}if(!w)for(x=this.parsers.length;x--&&!w;){var y=this.parsers[x];(E=y.regex.exec(g))&&(w=y.parse(E))}}this.rgba=w||[]},get:function(c){var g=this.input,x=this.rgba;if(this.stops){var E=H(g);E.stops=
[].concat(E.stops);this.stops.forEach(function(g,t){E.stops[t]=[E.stops[t][0],g.get(c)]})}else E=x&&I(x[0])?"rgb"===c||!c&&1===x[3]?"rgb("+x[0]+","+x[1]+","+x[2]+")":"a"===c?x[3]:"rgba("+x.join(",")+")":g;return E},brighten:function(c){var g,x=this.rgba;if(this.stops)this.stops.forEach(function(g){g.brighten(c)});else if(I(c)&&0!==c)for(g=0;3>g;g++)x[g]+=G(255*c),0>x[g]&&(x[g]=0),255<x[g]&&(x[g]=255);return this},setOpacity:function(c){this.rgba[3]=c;return this},tweenTo:function(c,g){var x=this.rgba,
w=c.rgba;w.length&&x&&x.length?(c=1!==w[3]||1!==x[3],g=(c?"rgba(":"rgb(")+Math.round(w[0]+(x[0]-w[0])*(1-g))+","+Math.round(w[1]+(x[1]-w[1])*(1-g))+","+Math.round(w[2]+(x[2]-w[2])*(1-g))+(c?","+(w[3]+(x[3]-w[3])*(1-g)):"")+")"):g=c.input||"none";return g}};c.color=function(g){return new c.Color(g)}});K(D,"parts/SvgRenderer.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.attr,G=g.defined,H=g.destroyObjectProperties,y=g.erase,w=g.extend,x=g.isArray,E=g.isNumber,F=g.isObject,
t=g.isString,m=g.objectEach,p=g.pick,q=g.pInt,h=g.splat,a=c.addEvent,b=c.animate,d=c.charts,e=c.color,k=c.css,C=c.createElement,z=c.deg2rad,r=c.doc,n=c.hasTouch,f=c.isFirefox,l=c.isMS,v=c.isWebKit,B=c.merge,A=c.noop,u=c.removeEvent,J=c.stop,L=c.svg,U=c.SVG_NS,Q=c.symbolSizes,P=c.win;var M=c.SVGElement=function(){return this};w(M.prototype,{opacity:1,SVG_NS:U,textProps:"direction fontSize fontWeight fontFamily fontStyle color lineHeight width textAlign textDecoration textOverflow textOutline cursor".split(" "),
init:function(a,b){this.element="span"===b?C(b):r.createElementNS(this.SVG_NS,b);this.renderer=a;c.fireEvent(this,"afterInit")},animate:function(a,f,l){var u=c.animObject(p(f,this.renderer.globalAnimation,!0));p(r.hidden,r.msHidden,r.webkitHidden,!1)&&(u.duration=0);0!==u.duration?(l&&(u.complete=l),b(this,a,u)):(this.attr(a,void 0,l),m(a,function(a,b){u.step&&u.step.call(this,a,{prop:b,pos:1})},this));return this},complexColor:function(a,b,f){var l=this.renderer,u,A,d,k,N,e,n,v,h,O,r,J=[],C;c.fireEvent(this.renderer,
"complexColor",{args:arguments},function(){a.radialGradient?A="radialGradient":a.linearGradient&&(A="linearGradient");A&&(d=a[A],N=l.gradients,n=a.stops,O=f.radialReference,x(d)&&(a[A]=d={x1:d[0],y1:d[1],x2:d[2],y2:d[3],gradientUnits:"userSpaceOnUse"}),"radialGradient"===A&&O&&!G(d.gradientUnits)&&(k=d,d=B(d,l.getRadialAttr(O,k),{gradientUnits:"userSpaceOnUse"})),m(d,function(a,b){"id"!==b&&J.push(b,a)}),m(n,function(a){J.push(a)}),J=J.join(","),N[J]?r=N[J].attr("id"):(d.id=r=c.uniqueKey(),N[J]=e=
l.createElement(A).attr(d).add(l.defs),e.radAttr=k,e.stops=[],n.forEach(function(a){0===a[1].indexOf("rgba")?(u=c.color(a[1]),v=u.get("rgb"),h=u.get("a")):(v=a[1],h=1);a=l.createElement("stop").attr({offset:a[0],"stop-color":v,"stop-opacity":h}).add(e);e.stops.push(a)})),C="url("+l.url+"#"+r+")",f.setAttribute(b,C),f.gradient=J,a.toString=function(){return C})})},applyTextOutline:function(a){var b=this.element,f;-1!==a.indexOf("contrast")&&(a=a.replace(/contrast/g,this.renderer.getContrast(b.style.fill)));
a=a.split(" ");var l=a[a.length-1];if((f=a[0])&&"none"!==f&&c.svg){this.fakeTS=!0;a=[].slice.call(b.getElementsByTagName("tspan"));this.ySetter=this.xSetter;f=f.replace(/(^[\d\.]+)(.*?)$/g,function(a,b,f){return 2*b+f});this.removeTextOutline(a);var u=b.firstChild;a.forEach(function(a,A){0===A&&(a.setAttribute("x",b.getAttribute("x")),A=b.getAttribute("y"),a.setAttribute("y",A||0),null===A&&b.setAttribute("y",0));a=a.cloneNode(1);I(a,{"class":"highcharts-text-outline",fill:l,stroke:l,"stroke-width":f,
"stroke-linejoin":"round"});b.insertBefore(a,u)})}},removeTextOutline:function(a){for(var b=a.length,f;b--;)f=a[b],"highcharts-text-outline"===f.getAttribute("class")&&y(a,this.element.removeChild(f))},symbolCustomAttribs:"x y width height r start end innerR anchorX anchorY rounded".split(" "),attr:function(a,b,f,l){var u=this.element,A,d=this,k,e,N=this.symbolCustomAttribs;if("string"===typeof a&&void 0!==b){var n=a;a={};a[n]=b}"string"===typeof a?d=(this[a+"Getter"]||this._defaultGetter).call(this,
a,u):(m(a,function(b,f){k=!1;l||J(this,f);this.symbolName&&-1!==c.inArray(f,N)&&(A||(this.symbolAttr(a),A=!0),k=!0);!this.rotation||"x"!==f&&"y"!==f||(this.doTransform=!0);k||(e=this[f+"Setter"]||this._defaultSetter,e.call(this,b,f,u),!this.styledMode&&this.shadows&&/^(width|height|visibility|x|y|d|transform|cx|cy|r)$/.test(f)&&this.updateShadows(f,b,e))},this),this.afterSetters());f&&f.call(this);return d},afterSetters:function(){this.doTransform&&(this.updateTransform(),this.doTransform=!1)},updateShadows:function(a,
b,f){for(var l=this.shadows,u=l.length;u--;)f.call(l[u],"height"===a?Math.max(b-(l[u].cutHeight||0),0):"d"===a?this.d:b,a,l[u])},addClass:function(a,b){var f=b?"":this.attr("class")||"";a=(a||"").split(/ /g).reduce(function(a,b){-1===f.indexOf(b)&&a.push(b);return a},f?[f]:[]).join(" ");a!==f&&this.attr("class",a);return this},hasClass:function(a){return-1!==(this.attr("class")||"").split(" ").indexOf(a)},removeClass:function(a){return this.attr("class",(this.attr("class")||"").replace(t(a)?new RegExp(" ?"+
a+" ?"):a,""))},symbolAttr:function(a){var b=this;"x y r start end width height innerR anchorX anchorY clockwise".split(" ").forEach(function(f){b[f]=p(a[f],b[f])});b.attr({d:b.renderer.symbols[b.symbolName](b.x,b.y,b.width,b.height,b)})},clip:function(a){return this.attr("clip-path",a?"url("+this.renderer.url+"#"+a.id+")":"none")},crisp:function(a,b){b=b||a.strokeWidth||0;var f=Math.round(b)%2/2;a.x=Math.floor(a.x||this.x||0)+f;a.y=Math.floor(a.y||this.y||0)+f;a.width=Math.floor((a.width||this.width||
0)-2*f);a.height=Math.floor((a.height||this.height||0)-2*f);G(a.strokeWidth)&&(a.strokeWidth=b);return a},css:function(a){var b=this.styles,f={},l=this.element,u="",A=!b,d=["textOutline","textOverflow","width"];a&&a.color&&(a.fill=a.color);b&&m(a,function(a,l){a!==b[l]&&(f[l]=a,A=!0)});if(A){b&&(a=w(b,f));if(a)if(null===a.width||"auto"===a.width)delete this.textWidth;else if("text"===l.nodeName.toLowerCase()&&a.width)var e=this.textWidth=q(a.width);this.styles=a;e&&!L&&this.renderer.forExport&&delete a.width;
if(l.namespaceURI===this.SVG_NS){var n=function(a,b){return"-"+b.toLowerCase()};m(a,function(a,b){-1===d.indexOf(b)&&(u+=b.replace(/([A-Z])/g,n)+":"+a+";")});u&&I(l,"style",u)}else k(l,a);this.added&&("text"===this.element.nodeName&&this.renderer.buildText(this),a&&a.textOutline&&this.applyTextOutline(a.textOutline))}return this},getStyle:function(a){return P.getComputedStyle(this.element||this,"").getPropertyValue(a)},strokeWidth:function(){if(!this.renderer.styledMode)return this["stroke-width"]||
0;var a=this.getStyle("stroke-width");if(a.indexOf("px")===a.length-2)a=q(a);else{var b=r.createElementNS(U,"rect");I(b,{width:a,"stroke-width":0});this.element.parentNode.appendChild(b);a=b.getBBox().width;b.parentNode.removeChild(b)}return a},on:function(a,b){var f=this,l=f.element;n&&"click"===a?(l.ontouchstart=function(a){f.touchEventFired=Date.now();a.preventDefault();b.call(l,a)},l.onclick=function(a){(-1===P.navigator.userAgent.indexOf("Android")||1100<Date.now()-(f.touchEventFired||0))&&b.call(l,
a)}):l["on"+a]=b;return this},setRadialReference:function(a){var b=this.renderer.gradients[this.element.gradient];this.element.radialReference=a;b&&b.radAttr&&b.animate(this.renderer.getRadialAttr(a,b.radAttr));return this},translate:function(a,b){return this.attr({translateX:a,translateY:b})},invert:function(a){this.inverted=a;this.updateTransform();return this},updateTransform:function(){var a=this.translateX||0,b=this.translateY||0,f=this.scaleX,l=this.scaleY,u=this.inverted,A=this.rotation,d=
this.matrix,k=this.element;u&&(a+=this.width,b+=this.height);a=["translate("+a+","+b+")"];G(d)&&a.push("matrix("+d.join(",")+")");u?a.push("rotate(90) scale(-1,1)"):A&&a.push("rotate("+A+" "+p(this.rotationOriginX,k.getAttribute("x"),0)+" "+p(this.rotationOriginY,k.getAttribute("y")||0)+")");(G(f)||G(l))&&a.push("scale("+p(f,1)+" "+p(l,1)+")");a.length&&k.setAttribute("transform",a.join(" "))},toFront:function(){var a=this.element;a.parentNode.appendChild(a);return this},align:function(a,b,f){var l,
u={};var A=this.renderer;var d=A.alignedObjects;var k,e;if(a){if(this.alignOptions=a,this.alignByTranslate=b,!f||t(f))this.alignTo=l=f||"renderer",y(d,this),d.push(this),f=null}else a=this.alignOptions,b=this.alignByTranslate,l=this.alignTo;f=p(f,A[l],A);l=a.align;A=a.verticalAlign;d=(f.x||0)+(a.x||0);var n=(f.y||0)+(a.y||0);"right"===l?k=1:"center"===l&&(k=2);k&&(d+=(f.width-(a.width||0))/k);u[b?"translateX":"x"]=Math.round(d);"bottom"===A?e=1:"middle"===A&&(e=2);e&&(n+=(f.height-(a.height||0))/
e);u[b?"translateY":"y"]=Math.round(n);this[this.placed?"animate":"attr"](u);this.placed=!0;this.alignAttr=u;return this},getBBox:function(a,b){var f,l=this.renderer,u=this.element,A=this.styles,d=this.textStr,k,e=l.cache,n=l.cacheKeys,N=u.namespaceURI===this.SVG_NS;b=p(b,this.rotation,0);var v=l.styledMode?u&&M.prototype.getStyle.call(u,"font-size"):A&&A.fontSize;if(G(d)){var B=d.toString();-1===B.indexOf("<")&&(B=B.replace(/[0-9]/g,"0"));B+=["",b,v,this.textWidth,A&&A.textOverflow].join()}B&&!a&&
(f=e[B]);if(!f){if(N||l.forExport){try{(k=this.fakeTS&&function(a){[].forEach.call(u.querySelectorAll(".highcharts-text-outline"),function(b){b.style.display=a})})&&k("none"),f=u.getBBox?w({},u.getBBox()):{width:u.offsetWidth,height:u.offsetHeight},k&&k("")}catch(Y){""}if(!f||0>f.width)f={width:0,height:0}}else f=this.htmlGetBBox();l.isSVG&&(a=f.width,l=f.height,N&&(f.height=l={"11px,17":14,"13px,20":16}[A&&A.fontSize+","+Math.round(l)]||l),b&&(A=b*z,f.width=Math.abs(l*Math.sin(A))+Math.abs(a*Math.cos(A)),
f.height=Math.abs(l*Math.cos(A))+Math.abs(a*Math.sin(A))));if(B&&0<f.height){for(;250<n.length;)delete e[n.shift()];e[B]||n.push(B);e[B]=f}}return f},show:function(a){return this.attr({visibility:a?"inherit":"visible"})},hide:function(a){a?this.attr({y:-9999}):this.attr({visibility:"hidden"});return this},fadeOut:function(a){var b=this;b.animate({opacity:0},{duration:a||150,complete:function(){b.attr({y:-9999})}})},add:function(a){var b=this.renderer,f=this.element;a&&(this.parentGroup=a);this.parentInverted=
a&&a.inverted;void 0!==this.textStr&&b.buildText(this);this.added=!0;if(!a||a.handleZ||this.zIndex)var l=this.zIndexSetter();l||(a?a.element:b.box).appendChild(f);if(this.onAdd)this.onAdd();return this},safeRemoveChild:function(a){var b=a.parentNode;b&&b.removeChild(a)},destroy:function(){var a=this,b=a.element||{},f=a.renderer,l=f.isSVG&&"SPAN"===b.nodeName&&a.parentGroup,u=b.ownerSVGElement,A=a.clipPath;b.onclick=b.onmouseout=b.onmouseover=b.onmousemove=b.point=null;J(a);A&&u&&([].forEach.call(u.querySelectorAll("[clip-path],[CLIP-PATH]"),
function(a){-1<a.getAttribute("clip-path").indexOf(A.element.id)&&a.removeAttribute("clip-path")}),a.clipPath=A.destroy());if(a.stops){for(u=0;u<a.stops.length;u++)a.stops[u]=a.stops[u].destroy();a.stops=null}a.safeRemoveChild(b);for(f.styledMode||a.destroyShadows();l&&l.div&&0===l.div.childNodes.length;)b=l.parentGroup,a.safeRemoveChild(l.div),delete l.div,l=b;a.alignTo&&y(f.alignedObjects,a);m(a,function(b,f){a[f]&&a[f].parentGroup===a&&a[f].destroy&&a[f].destroy();delete a[f]})},shadow:function(a,
b,f){var l=[],u,A=this.element;if(!a)this.destroyShadows();else if(!this.shadows){var d=p(a.width,3);var k=(a.opacity||.15)/d;var e=this.parentInverted?"(-1,-1)":"("+p(a.offsetX,1)+", "+p(a.offsetY,1)+")";for(u=1;u<=d;u++){var n=A.cloneNode(0);var v=2*d+1-2*u;I(n,{stroke:a.color||"#000000","stroke-opacity":k*u,"stroke-width":v,transform:"translate"+e,fill:"none"});n.setAttribute("class",(n.getAttribute("class")||"")+" highcharts-shadow");f&&(I(n,"height",Math.max(I(n,"height")-v,0)),n.cutHeight=v);
b?b.element.appendChild(n):A.parentNode&&A.parentNode.insertBefore(n,A);l.push(n)}this.shadows=l}return this},destroyShadows:function(){(this.shadows||[]).forEach(function(a){this.safeRemoveChild(a)},this);this.shadows=void 0},xGetter:function(a){"circle"===this.element.nodeName&&("x"===a?a="cx":"y"===a&&(a="cy"));return this._defaultGetter(a)},_defaultGetter:function(a){a=p(this[a+"Value"],this[a],this.element?this.element.getAttribute(a):null,0);/^[\-0-9\.]+$/.test(a)&&(a=parseFloat(a));return a},
dSetter:function(a,b,f){a&&a.join&&(a=a.join(" "));/(NaN| {2}|^$)/.test(a)&&(a="M 0 0");this[b]!==a&&(f.setAttribute(b,a),this[b]=a)},dashstyleSetter:function(a){var b,f=this["stroke-width"];"inherit"===f&&(f=1);if(a=a&&a.toLowerCase()){a=a.replace("shortdashdotdot","3,1,1,1,1,1,").replace("shortdashdot","3,1,1,1").replace("shortdot","1,1,").replace("shortdash","3,1,").replace("longdash","8,3,").replace(/dot/g,"1,3,").replace("dash","4,3,").replace(/,$/,"").split(",");for(b=a.length;b--;)a[b]=q(a[b])*
f;a=a.join(",").replace(/NaN/g,"none");this.element.setAttribute("stroke-dasharray",a)}},alignSetter:function(a){var b={left:"start",center:"middle",right:"end"};b[a]&&(this.alignValue=a,this.element.setAttribute("text-anchor",b[a]))},opacitySetter:function(a,b,f){this[b]=a;f.setAttribute(b,a)},titleSetter:function(a){var b=this.element.getElementsByTagName("title")[0];b||(b=r.createElementNS(this.SVG_NS,"title"),this.element.appendChild(b));b.firstChild&&b.removeChild(b.firstChild);b.appendChild(r.createTextNode(String(p(a,
"")).replace(/<[^>]*>/g,"").replace(/&lt;/g,"<").replace(/&gt;/g,">")))},textSetter:function(a){a!==this.textStr&&(delete this.bBox,delete this.textPxLength,this.textStr=a,this.added&&this.renderer.buildText(this))},setTextPath:function(a,b){var f=this.element,l={textAnchor:"text-anchor"},u=!1,d=this.textPathWrapper,k=!d;b=B(!0,{enabled:!0,attributes:{dy:-5,startOffset:"50%",textAnchor:"middle"}},b);var e=b.attributes;if(a&&b&&b.enabled){this.options&&this.options.padding&&(e.dx=-this.options.padding);
d||(this.textPathWrapper=d=this.renderer.createElement("textPath"),u=!0);var n=d.element;(b=a.element.getAttribute("id"))||a.element.setAttribute("id",b=c.uniqueKey());if(k)for(a=f.getElementsByTagName("tspan");a.length;)a[0].setAttribute("y",0),n.appendChild(a[0]);u&&d.add({element:this.text?this.text.element:f});n.setAttributeNS("http://www.w3.org/1999/xlink","href",this.renderer.url+"#"+b);G(e.dy)&&(n.parentNode.setAttribute("dy",e.dy),delete e.dy);G(e.dx)&&(n.parentNode.setAttribute("dx",e.dx),
delete e.dx);m(e,function(a,b){n.setAttribute(l[b]||b,a)});f.removeAttribute("transform");this.removeTextOutline.call(d,[].slice.call(f.getElementsByTagName("tspan")));this.text&&!this.renderer.styledMode&&this.attr({fill:"none","stroke-width":0});this.applyTextOutline=this.updateTransform=A}else d&&(delete this.updateTransform,delete this.applyTextOutline,this.destroyTextPath(f,a));return this},destroyTextPath:function(a,b){var f;b.element.setAttribute("id","");for(f=this.textPathWrapper.element.childNodes;f.length;)a.firstChild.appendChild(f[0]);
a.firstChild.removeChild(this.textPathWrapper.element);delete b.textPathWrapper},fillSetter:function(a,b,f){"string"===typeof a?f.setAttribute(b,a):a&&this.complexColor(a,b,f)},visibilitySetter:function(a,b,f){"inherit"===a?f.removeAttribute(b):this[b]!==a&&f.setAttribute(b,a);this[b]=a},zIndexSetter:function(a,b){var f=this.renderer,l=this.parentGroup,u=(l||f).element||f.box,A=this.element,d=!1;f=u===f.box;var k=this.added;var e;G(a)?(A.setAttribute("data-z-index",a),a=+a,this[b]===a&&(k=!1)):G(this[b])&&
A.removeAttribute("data-z-index");this[b]=a;if(k){(a=this.zIndex)&&l&&(l.handleZ=!0);b=u.childNodes;for(e=b.length-1;0<=e&&!d;e--){l=b[e];k=l.getAttribute("data-z-index");var n=!G(k);if(l!==A)if(0>a&&n&&!f&&!e)u.insertBefore(A,b[e]),d=!0;else if(q(k)<=a||n&&(!G(a)||0<=a))u.insertBefore(A,b[e+1]||null),d=!0}d||(u.insertBefore(A,b[f?3:0]||null),d=!0)}return d},_defaultSetter:function(a,b,f){f.setAttribute(b,a)}});M.prototype.yGetter=M.prototype.xGetter;M.prototype.translateXSetter=M.prototype.translateYSetter=
M.prototype.rotationSetter=M.prototype.verticalAlignSetter=M.prototype.rotationOriginXSetter=M.prototype.rotationOriginYSetter=M.prototype.scaleXSetter=M.prototype.scaleYSetter=M.prototype.matrixSetter=function(a,b){this[b]=a;this.doTransform=!0};M.prototype["stroke-widthSetter"]=M.prototype.strokeSetter=function(a,b,f){this[b]=a;this.stroke&&this["stroke-width"]?(M.prototype.fillSetter.call(this,this.stroke,"stroke",f),f.setAttribute("stroke-width",this["stroke-width"]),this.hasStroke=!0):"stroke-width"===
b&&0===a&&this.hasStroke?(f.removeAttribute("stroke"),this.hasStroke=!1):this.renderer.styledMode&&this["stroke-width"]&&(f.setAttribute("stroke-width",this["stroke-width"]),this.hasStroke=!0)};g=c.SVGRenderer=function(){this.init.apply(this,arguments)};w(g.prototype,{Element:M,SVG_NS:U,init:function(b,l,u,A,d,e,n){var B=this.createElement("svg").attr({version:"1.1","class":"highcharts-root"});n||B.css(this.getStyle(A));A=B.element;b.appendChild(A);I(b,"dir","ltr");-1===b.innerHTML.indexOf("xmlns")&&
I(A,"xmlns",this.SVG_NS);this.isSVG=!0;this.box=A;this.boxWrapper=B;this.alignedObjects=[];this.url=(f||v)&&r.getElementsByTagName("base").length?P.location.href.split("#")[0].replace(/<[^>]*>/g,"").replace(/([\('\)])/g,"\\$1").replace(/ /g,"%20"):"";this.createElement("desc").add().element.appendChild(r.createTextNode("Created with Highcharts 7.2.1"));this.defs=this.createElement("defs").add();this.allowHTML=e;this.forExport=d;this.styledMode=n;this.gradients={};this.cache={};this.cacheKeys=[];this.imgCount=
0;this.setSize(l,u,!1);var h;f&&b.getBoundingClientRect&&(l=function(){k(b,{left:0,top:0});h=b.getBoundingClientRect();k(b,{left:Math.ceil(h.left)-h.left+"px",top:Math.ceil(h.top)-h.top+"px"})},l(),this.unSubPixelFix=a(P,"resize",l))},definition:function(a){function b(a,l){var u;h(a).forEach(function(a){var A=f.createElement(a.tagName),d={};m(a,function(a,b){"tagName"!==b&&"children"!==b&&"textContent"!==b&&(d[b]=a)});A.attr(d);A.add(l||f.defs);a.textContent&&A.element.appendChild(r.createTextNode(a.textContent));
b(a.children||[],A);u=A});return u}var f=this;return b(a)},getStyle:function(a){return this.style=w({fontFamily:'"Lucida Grande", "Lucida Sans Unicode", Arial, Helvetica, sans-serif',fontSize:"12px"},a)},setStyle:function(a){this.boxWrapper.css(this.getStyle(a))},isHidden:function(){return!this.boxWrapper.getBBox().width},destroy:function(){var a=this.defs;this.box=null;this.boxWrapper=this.boxWrapper.destroy();H(this.gradients||{});this.gradients=null;a&&(this.defs=a.destroy());this.unSubPixelFix&&
this.unSubPixelFix();return this.alignedObjects=null},createElement:function(a){var b=new this.Element;b.init(this,a);return b},draw:A,getRadialAttr:function(a,b){return{cx:a[0]-a[2]/2+b.cx*a[2],cy:a[1]-a[2]/2+b.cy*a[2],r:b.r*a[2]}},truncate:function(a,b,f,l,u,A,d){var e=this,k=a.rotation,n,v=l?1:0,B=(f||l).length,h=B,c=[],J=function(a){b.firstChild&&b.removeChild(b.firstChild);a&&b.appendChild(r.createTextNode(a))},C=function(A,k){k=k||A;if(void 0===c[k])if(b.getSubStringLength)try{c[k]=u+b.getSubStringLength(0,
l?k+1:k)}catch(fa){""}else e.getSpanWidth&&(J(d(f||l,A)),c[k]=u+e.getSpanWidth(a,b));return c[k]},m;a.rotation=0;var L=C(b.textContent.length);if(m=u+L>A){for(;v<=B;)h=Math.ceil((v+B)/2),l&&(n=d(l,h)),L=C(h,n&&n.length-1),v===B?v=B+1:L>A?B=h-1:v=h;0===B?J(""):f&&B===f.length-1||J(n||d(f||l,h))}l&&l.splice(0,h);a.actualWidth=L;a.rotation=k;return m},escapes:{"&":"&amp;","<":"&lt;",">":"&gt;","'":"&#39;",'"':"&quot;"},buildText:function(a){var b=a.element,f=this,l=f.forExport,u=p(a.textStr,"").toString(),
A=-1!==u.indexOf("<"),d=b.childNodes,e,n=I(b,"x"),v=a.styles,B=a.textWidth,h=v&&v.lineHeight,c=v&&v.textOutline,J=v&&"ellipsis"===v.textOverflow,C=v&&"nowrap"===v.whiteSpace,N=v&&v.fontSize,z,g=d.length;v=B&&!a.added&&this.box;var t=function(a){var l;f.styledMode||(l=/(px|em)$/.test(a&&a.style.fontSize)?a.style.fontSize:N||f.style.fontSize||12);return h?q(h):f.fontMetrics(l,a.getAttribute("style")?a:b).h},Q=function(a,b){m(f.escapes,function(f,l){b&&-1!==b.indexOf(f)||(a=a.toString().replace(new RegExp(f,
"g"),l))});return a},P=function(a,b){var f=a.indexOf("<");a=a.substring(f,a.indexOf(">")-f);f=a.indexOf(b+"=");if(-1!==f&&(f=f+b.length+1,b=a.charAt(f),'"'===b||"'"===b))return a=a.substring(f+1),a.substring(0,a.indexOf(b))},x=/<br.*?>/g;var M=[u,J,C,h,c,N,B].join();if(M!==a.textCache){for(a.textCache=M;g--;)b.removeChild(d[g]);A||c||J||B||-1!==u.indexOf(" ")&&(!C||x.test(u))?(v&&v.appendChild(b),A?(u=f.styledMode?u.replace(/<(b|strong)>/g,'<span class="highcharts-strong">').replace(/<(i|em)>/g,'<span class="highcharts-emphasized">'):
u.replace(/<(b|strong)>/g,'<span style="font-weight:bold">').replace(/<(i|em)>/g,'<span style="font-style:italic">'),u=u.replace(/<a/g,"<span").replace(/<\/(b|strong|i|em|a)>/g,"</span>").split(x)):u=[u],u=u.filter(function(a){return""!==a}),u.forEach(function(u,A){var d=0,v=0;u=u.replace(/^\s+|\s+$/g,"").replace(/<span/g,"|||<span").replace(/<\/span>/g,"</span>|||");var h=u.split("|||");h.forEach(function(u){if(""!==u||1===h.length){var c={},m=r.createElementNS(f.SVG_NS,"tspan"),p,O;(p=P(u,"class"))&&
I(m,"class",p);if(p=P(u,"style"))p=p.replace(/(;| |^)color([ :])/,"$1fill$2"),I(m,"style",p);(O=P(u,"href"))&&!l&&(I(m,"onclick",'location.href="'+O+'"'),I(m,"class","highcharts-anchor"),f.styledMode||k(m,{cursor:"pointer"}));u=Q(u.replace(/<[a-zA-Z\/](.|\n)*?>/g,"")||" ");if(" "!==u){m.appendChild(r.createTextNode(u));d?c.dx=0:A&&null!==n&&(c.x=n);I(m,c);b.appendChild(m);!d&&z&&(!L&&l&&k(m,{display:"block"}),I(m,"dy",t(m)));if(B){var q=u.replace(/([^\^])-/g,"$1- ").split(" ");c=!C&&(1<h.length||
A||1<q.length);O=0;var g=t(m);if(J)e=f.truncate(a,m,u,void 0,0,Math.max(0,B-parseInt(N||12,10)),function(a,b){return a.substring(0,b)+"\u2026"});else if(c)for(;q.length;)q.length&&!C&&0<O&&(m=r.createElementNS(U,"tspan"),I(m,{dy:g,x:n}),p&&I(m,"style",p),m.appendChild(r.createTextNode(q.join(" ").replace(/- /g,"-"))),b.appendChild(m)),f.truncate(a,m,null,q,0===O?v:0,B,function(a,b){return q.slice(0,b).join(" ").replace(/- /g,"-")}),v=a.actualWidth,O++}d++}}});z=z||b.childNodes.length}),J&&e&&a.attr("title",
Q(a.textStr,["&lt;","&gt;"])),v&&v.removeChild(b),c&&a.applyTextOutline&&a.applyTextOutline(c)):b.appendChild(r.createTextNode(Q(u)))}},getContrast:function(a){a=e(a).rgba;a[0]*=1;a[1]*=1.2;a[2]*=.5;return 459<a[0]+a[1]+a[2]?"#000000":"#FFFFFF"},button:function(b,f,u,A,d,k,e,n,v,h){var c=this.label(b,f,u,v,null,null,h,null,"button"),r=0,J=this.styledMode;c.attr(B({padding:8,r:2},d));if(!J){d=B({fill:"#f7f7f7",stroke:"#cccccc","stroke-width":1,style:{color:"#333333",cursor:"pointer",fontWeight:"normal"}},
d);var m=d.style;delete d.style;k=B(d,{fill:"#e6e6e6"},k);var C=k.style;delete k.style;e=B(d,{fill:"#e6ebf5",style:{color:"#000000",fontWeight:"bold"}},e);var L=e.style;delete e.style;n=B(d,{style:{color:"#cccccc"}},n);var N=n.style;delete n.style}a(c.element,l?"mouseover":"mouseenter",function(){3!==r&&c.setState(1)});a(c.element,l?"mouseout":"mouseleave",function(){3!==r&&c.setState(r)});c.setState=function(a){1!==a&&(c.state=r=a);c.removeClass(/highcharts-button-(normal|hover|pressed|disabled)/).addClass("highcharts-button-"+
["normal","hover","pressed","disabled"][a||0]);J||c.attr([d,k,e,n][a||0]).css([m,C,L,N][a||0])};J||c.attr(d).css(w({cursor:"default"},m));return c.on("click",function(a){3!==r&&A.call(c,a)})},crispLine:function(a,b){a[1]===a[4]&&(a[1]=a[4]=Math.round(a[1])-b%2/2);a[2]===a[5]&&(a[2]=a[5]=Math.round(a[2])+b%2/2);return a},path:function(a){var b=this.styledMode?{}:{fill:"none"};x(a)?b.d=a:F(a)&&w(b,a);return this.createElement("path").attr(b)},circle:function(a,b,f){a=F(a)?a:void 0===a?{}:{x:a,y:b,r:f};
b=this.createElement("circle");b.xSetter=b.ySetter=function(a,b,f){f.setAttribute("c"+b,a)};return b.attr(a)},arc:function(a,b,f,l,u,A){F(a)?(l=a,b=l.y,f=l.r,a=l.x):l={innerR:l,start:u,end:A};a=this.symbol("arc",a,b,f,f,l);a.r=f;return a},rect:function(a,b,f,l,u,A){u=F(a)?a.r:u;var d=this.createElement("rect");a=F(a)?a:void 0===a?{}:{x:a,y:b,width:Math.max(f,0),height:Math.max(l,0)};this.styledMode||(void 0!==A&&(a.strokeWidth=A,a=d.crisp(a)),a.fill="none");u&&(a.r=u);d.rSetter=function(a,b,f){d.r=
a;I(f,{rx:a,ry:a})};d.rGetter=function(){return d.r};return d.attr(a)},setSize:function(a,b,f){var l=this.alignedObjects,u=l.length;this.width=a;this.height=b;for(this.boxWrapper.animate({width:a,height:b},{step:function(){this.attr({viewBox:"0 0 "+this.attr("width")+" "+this.attr("height")})},duration:p(f,!0)?void 0:0});u--;)l[u].align()},g:function(a){var b=this.createElement("g");return a?b.attr({"class":"highcharts-"+a}):b},image:function(b,f,l,u,A,d){var k={preserveAspectRatio:"none"},e=function(a,
b){a.setAttributeNS?a.setAttributeNS("http://www.w3.org/1999/xlink","href",b):a.setAttribute("hc-svg-href",b)},n=function(a){e(v.element,b);d.call(v,a)};1<arguments.length&&w(k,{x:f,y:l,width:u,height:A});var v=this.createElement("image").attr(k);d?(e(v.element,"data:image/gif;base64,R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw=="),k=new P.Image,a(k,"load",n),k.src=b,k.complete&&n({})):e(v.element,b);return v},symbol:function(a,b,f,l,u,A){var e=this,n=/^url\((.*?)\)$/,v=n.test(a),B=!v&&(this.symbols[a]?
a:"circle"),h=B&&this.symbols[B],c=G(b)&&h&&h.call(this.symbols,Math.round(b),Math.round(f),l,u,A);if(h){var J=this.path(c);e.styledMode||J.attr("fill","none");w(J,{symbolName:B,x:b,y:f,width:l,height:u});A&&w(J,A)}else if(v){var m=a.match(n)[1];J=this.image(m);J.imgwidth=p(Q[m]&&Q[m].width,A&&A.width);J.imgheight=p(Q[m]&&Q[m].height,A&&A.height);var L=function(){J.attr({width:J.width,height:J.height})};["width","height"].forEach(function(a){J[a+"Setter"]=function(a,b){var f={},l=this["img"+b],u=
"width"===b?"translateX":"translateY";this[b]=a;G(l)&&(A&&"within"===A.backgroundSize&&this.width&&this.height&&(l=Math.round(l*Math.min(this.width/this.imgwidth,this.height/this.imgheight))),this.element&&this.element.setAttribute(b,l),this.alignByTranslate||(f[u]=((this[b]||0)-l)/2,this.attr(f)))}});G(b)&&J.attr({x:b,y:f});J.isImg=!0;G(J.imgwidth)&&G(J.imgheight)?L():(J.attr({width:0,height:0}),C("img",{onload:function(){var a=d[e.chartIndex];0===this.width&&(k(this,{position:"absolute",top:"-999em"}),
r.body.appendChild(this));Q[m]={width:this.width,height:this.height};J.imgwidth=this.width;J.imgheight=this.height;J.element&&L();this.parentNode&&this.parentNode.removeChild(this);e.imgCount--;if(!e.imgCount&&a&&a.onload)a.onload()},src:m}),this.imgCount++)}return J},symbols:{circle:function(a,b,f,l){return this.arc(a+f/2,b+l/2,f/2,l/2,{start:.5*Math.PI,end:2.5*Math.PI,open:!1})},square:function(a,b,f,l){return["M",a,b,"L",a+f,b,a+f,b+l,a,b+l,"Z"]},triangle:function(a,b,f,l){return["M",a+f/2,b,"L",
a+f,b+l,a,b+l,"Z"]},"triangle-down":function(a,b,f,l){return["M",a,b,"L",a+f,b,a+f/2,b+l,"Z"]},diamond:function(a,b,f,l){return["M",a+f/2,b,"L",a+f,b+l/2,a+f/2,b+l,a,b+l/2,"Z"]},arc:function(a,b,f,l,u){var A=u.start,d=u.r||f,k=u.r||l||f,e=u.end-.001;f=u.innerR;l=p(u.open,.001>Math.abs(u.end-u.start-2*Math.PI));var n=Math.cos(A),v=Math.sin(A),B=Math.cos(e);e=Math.sin(e);A=.001>u.end-A-Math.PI?0:1;u=["M",a+d*n,b+k*v,"A",d,k,0,A,p(u.clockwise,1),a+d*B,b+k*e];G(f)&&u.push(l?"M":"L",a+f*B,b+f*e,"A",f,
f,0,A,0,a+f*n,b+f*v);u.push(l?"":"Z");return u},callout:function(a,b,f,l,u){var A=Math.min(u&&u.r||0,f,l),d=A+6,e=u&&u.anchorX;u=u&&u.anchorY;var k=["M",a+A,b,"L",a+f-A,b,"C",a+f,b,a+f,b,a+f,b+A,"L",a+f,b+l-A,"C",a+f,b+l,a+f,b+l,a+f-A,b+l,"L",a+A,b+l,"C",a,b+l,a,b+l,a,b+l-A,"L",a,b+A,"C",a,b,a,b,a+A,b];e&&e>f?u>b+d&&u<b+l-d?k.splice(13,3,"L",a+f,u-6,a+f+6,u,a+f,u+6,a+f,b+l-A):k.splice(13,3,"L",a+f,l/2,e,u,a+f,l/2,a+f,b+l-A):e&&0>e?u>b+d&&u<b+l-d?k.splice(33,3,"L",a,u+6,a-6,u,a,u-6,a,b+A):k.splice(33,
3,"L",a,l/2,e,u,a,l/2,a,b+A):u&&u>l&&e>a+d&&e<a+f-d?k.splice(23,3,"L",e+6,b+l,e,b+l+6,e-6,b+l,a+A,b+l):u&&0>u&&e>a+d&&e<a+f-d&&k.splice(3,3,"L",e-6,b,e,b-6,e+6,b,f-A,b);return k}},clipRect:function(a,b,f,l){var u=c.uniqueKey()+"-",A=this.createElement("clipPath").attr({id:u}).add(this.defs);a=this.rect(a,b,f,l,0).add(A);a.id=u;a.clipPath=A;a.count=0;return a},text:function(a,b,f,l){var u={};if(l&&(this.allowHTML||!this.forExport))return this.html(a,b,f);u.x=Math.round(b||0);f&&(u.y=Math.round(f));
G(a)&&(u.text=a);a=this.createElement("text").attr(u);l||(a.xSetter=function(a,b,f){var l=f.getElementsByTagName("tspan"),u=f.getAttribute(b),A;for(A=0;A<l.length;A++){var d=l[A];d.getAttribute(b)===u&&d.setAttribute(b,a)}f.setAttribute(b,a)});return a},fontMetrics:function(a,b){a=!this.styledMode&&/px/.test(a)||!P.getComputedStyle?a||b&&b.style&&b.style.fontSize||this.style&&this.style.fontSize:b&&M.prototype.getStyle.call(b,"font-size");a=/px/.test(a)?q(a):12;b=24>a?a+3:Math.round(1.2*a);return{h:b,
b:Math.round(.8*b),f:a}},rotCorr:function(a,b,f){var l=a;b&&f&&(l=Math.max(l*Math.cos(b*z),4));return{x:-a/3*Math.sin(b*z),y:l}},label:function(a,b,f,l,A,d,e,k,n){var v=this,h=v.styledMode,c=v.g("button"!==n&&"label"),J=c.text=v.text("",0,0,e).attr({zIndex:1}),r,m,C=0,L=3,p=0,z,U,q,g,N,O={},t,Q,P=/^url\((.*?)\)$/.test(l),x=h||P,da=function(){return h?r.strokeWidth()%2/2:(t?parseInt(t,10):0)%2/2};n&&c.addClass("highcharts-"+n);var y=function(){var a=J.element.style,b={};m=(void 0===z||void 0===U||
N)&&G(J.textStr)&&J.getBBox();c.width=(z||m.width||0)+2*L+p;c.height=(U||m.height||0)+2*L;Q=L+Math.min(v.fontMetrics(a&&a.fontSize,J).b,m?m.height:Infinity);x&&(r||(c.box=r=v.symbols[l]||P?v.symbol(l):v.rect(),r.addClass(("button"===n?"":"highcharts-label-box")+(n?" highcharts-"+n+"-box":"")),r.add(c),a=da(),b.x=a,b.y=(k?-Q:0)+a),b.width=Math.round(c.width),b.height=Math.round(c.height),r.attr(w(b,O)),O={})};var S=function(){var a=p+L;var b=k?0:Q;G(z)&&m&&("center"===N||"right"===N)&&(a+={center:.5,
right:1}[N]*(z-m.width));if(a!==J.x||b!==J.y)J.attr("x",a),J.hasBoxWidthChanged&&(m=J.getBBox(!0),y()),void 0!==b&&J.attr("y",b);J.x=a;J.y=b};var F=function(a,b){r?r.attr(a,b):O[a]=b};c.onAdd=function(){J.add(c);c.attr({text:a||0===a?a:"",x:b,y:f});r&&G(A)&&c.attr({anchorX:A,anchorY:d})};c.widthSetter=function(a){z=E(a)?a:null};c.heightSetter=function(a){U=a};c["text-alignSetter"]=function(a){N=a};c.paddingSetter=function(a){G(a)&&a!==L&&(L=c.padding=a,S())};c.paddingLeftSetter=function(a){G(a)&&
a!==p&&(p=a,S())};c.alignSetter=function(a){a={left:0,center:.5,right:1}[a];a!==C&&(C=a,m&&c.attr({x:q}))};c.textSetter=function(a){void 0!==a&&J.attr({text:a});y();S()};c["stroke-widthSetter"]=function(a,b){a&&(x=!0);t=this["stroke-width"]=a;F(b,a)};h?c.rSetter=function(a,b){F(b,a)}:c.strokeSetter=c.fillSetter=c.rSetter=function(a,b){"r"!==b&&("fill"===b&&a&&(x=!0),c[b]=a);F(b,a)};c.anchorXSetter=function(a,b){A=c.anchorX=a;F(b,Math.round(a)-da()-q)};c.anchorYSetter=function(a,b){d=c.anchorY=a;F(b,
a-g)};c.xSetter=function(a){c.x=a;C&&(a-=C*((z||m.width)+2*L),c["forceAnimate:x"]=!0);q=Math.round(a);c.attr("translateX",q)};c.ySetter=function(a){g=c.y=Math.round(a);c.attr("translateY",g)};var H=c.css;e={css:function(a){if(a){var b={};a=B(a);c.textProps.forEach(function(f){void 0!==a[f]&&(b[f]=a[f],delete a[f])});J.css(b);"width"in b&&y();"fontSize"in b&&(y(),S())}return H.call(c,a)},getBBox:function(){return{width:m.width+2*L,height:m.height+2*L,x:m.x-L,y:m.y-L}},destroy:function(){u(c.element,
"mouseenter");u(c.element,"mouseleave");J&&(J=J.destroy());r&&(r=r.destroy());M.prototype.destroy.call(c);c=v=y=S=F=null}};h||(e.shadow=function(a){a&&(y(),r&&r.shadow(a));return c});return w(c,e)}});c.Renderer=g});K(D,"parts/Html.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.attr,G=g.defined,H=g.extend,y=g.pick,w=g.pInt,x=c.createElement,E=c.css,F=c.isFirefox,t=c.isMS,m=c.isWebKit,p=c.SVGElement;g=c.SVGRenderer;var q=c.win;H(p.prototype,{htmlCss:function(c){var a="SPAN"===
this.element.tagName&&c&&"width"in c,b=y(a&&c.width,void 0);if(a){delete c.width;this.textWidth=b;var d=!0}c&&"ellipsis"===c.textOverflow&&(c.whiteSpace="nowrap",c.overflow="hidden");this.styles=H(this.styles,c);E(this.element,c);d&&this.htmlUpdateTransform();return this},htmlGetBBox:function(){var c=this.element;return{x:c.offsetLeft,y:c.offsetTop,width:c.offsetWidth,height:c.offsetHeight}},htmlUpdateTransform:function(){if(this.added){var c=this.renderer,a=this.element,b=this.translateX||0,d=this.translateY||
0,e=this.x||0,k=this.y||0,m=this.textAlign||"left",p={left:0,center:.5,right:1}[m],r=this.styles,n=r&&r.whiteSpace;E(a,{marginLeft:b,marginTop:d});!c.styledMode&&this.shadows&&this.shadows.forEach(function(a){E(a,{marginLeft:b+1,marginTop:d+1})});this.inverted&&[].forEach.call(a.childNodes,function(b){c.invertChild(b,a)});if("SPAN"===a.tagName){r=this.rotation;var f=this.textWidth&&w(this.textWidth),l=[r,m,a.innerHTML,this.textWidth,this.textAlign].join(),v;(v=f!==this.oldTextWidth)&&!(v=f>this.oldTextWidth)&&
((v=this.textPxLength)||(E(a,{width:"",whiteSpace:n||"nowrap"}),v=a.offsetWidth),v=v>f);v&&(/[ \-]/.test(a.textContent||a.innerText)||"ellipsis"===a.style.textOverflow)?(E(a,{width:f+"px",display:"block",whiteSpace:n||"normal"}),this.oldTextWidth=f,this.hasBoxWidthChanged=!0):this.hasBoxWidthChanged=!1;l!==this.cTT&&(n=c.fontMetrics(a.style.fontSize,a).b,!G(r)||r===(this.oldRotation||0)&&m===this.oldAlign||this.setSpanRotation(r,p,n),this.getSpanCorrection(!G(r)&&this.textPxLength||a.offsetWidth,
n,p,r,m));E(a,{left:e+(this.xCorr||0)+"px",top:k+(this.yCorr||0)+"px"});this.cTT=l;this.oldRotation=r;this.oldAlign=m}}else this.alignOnAdd=!0},setSpanRotation:function(c,a,b){var d={},e=this.renderer.getTransformKey();d[e]=d.transform="rotate("+c+"deg)";d[e+(F?"Origin":"-origin")]=d.transformOrigin=100*a+"% "+b+"px";E(this.element,d)},getSpanCorrection:function(c,a,b){this.xCorr=-c*b;this.yCorr=-a}});H(g.prototype,{getTransformKey:function(){return t&&!/Edge/.test(q.navigator.userAgent)?"-ms-transform":
m?"-webkit-transform":F?"MozTransform":q.opera?"-o-transform":""},html:function(c,a,b){var d=this.createElement("span"),e=d.element,k=d.renderer,h=k.isSVG,m=function(a,b){["opacity","visibility"].forEach(function(f){a[f+"Setter"]=function(l,d,e){var A=a.div?a.div.style:b;p.prototype[f+"Setter"].call(this,l,d,e);A&&(A[d]=l)}});a.addedSetters=!0};d.textSetter=function(a){a!==e.innerHTML&&(delete this.bBox,delete this.oldTextWidth);this.textStr=a;e.innerHTML=y(a,"");d.doTransform=!0};h&&m(d,d.element.style);
d.xSetter=d.ySetter=d.alignSetter=d.rotationSetter=function(a,b){"align"===b&&(b="textAlign");d[b]=a;d.doTransform=!0};d.afterSetters=function(){this.doTransform&&(this.htmlUpdateTransform(),this.doTransform=!1)};d.attr({text:c,x:Math.round(a),y:Math.round(b)}).css({position:"absolute"});k.styledMode||d.css({fontFamily:this.style.fontFamily,fontSize:this.style.fontSize});e.style.whiteSpace="nowrap";d.css=d.htmlCss;h&&(d.add=function(a){var b=k.box.parentNode,f=[];if(this.parentGroup=a){var l=a.div;
if(!l){for(;a;)f.push(a),a=a.parentGroup;f.reverse().forEach(function(a){function e(b,f){a[f]=b;"translateX"===f?u.left=b+"px":u.top=b+"px";a.doTransform=!0}var A=I(a.element,"class");l=a.div=a.div||x("div",A?{className:A}:void 0,{position:"absolute",left:(a.translateX||0)+"px",top:(a.translateY||0)+"px",display:a.display,opacity:a.opacity,pointerEvents:a.styles&&a.styles.pointerEvents},l||b);var u=l.style;H(a,{classSetter:function(a){return function(b){this.element.setAttribute("class",b);a.className=
b}}(l),on:function(){f[0].div&&d.on.apply({element:f[0].div},arguments);return a},translateXSetter:e,translateYSetter:e});a.addedSetters||m(a)})}}else l=b;l.appendChild(e);d.added=!0;d.alignOnAdd&&d.htmlUpdateTransform();return d});return d}})});K(D,"parts/Time.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.extend,H=g.isObject,y=g.objectEach,w=g.pick,x=g.splat,E=c.merge,F=c.timeUnits,t=c.win;c.Time=function(c){this.update(c,!1)};c.Time.prototype={defaultOptions:{Date:void 0,
getTimezoneOffset:void 0,timezone:void 0,timezoneOffset:0,useUTC:!0},update:function(c){var m=w(c&&c.useUTC,!0),q=this;this.options=c=E(!0,this.options||{},c);this.Date=c.Date||t.Date||Date;this.timezoneOffset=(this.useUTC=m)&&c.timezoneOffset;this.getTimezoneOffset=this.timezoneOffsetFunction();(this.variableTimezone=!(m&&!c.getTimezoneOffset&&!c.timezone))||this.timezoneOffset?(this.get=function(c,a){var b=a.getTime(),d=b-q.getTimezoneOffset(a);a.setTime(d);c=a["getUTC"+c]();a.setTime(b);return c},
this.set=function(c,a,b){if("Milliseconds"===c||"Seconds"===c||"Minutes"===c&&0===a.getTimezoneOffset()%60)a["set"+c](b);else{var d=q.getTimezoneOffset(a);d=a.getTime()-d;a.setTime(d);a["setUTC"+c](b);c=q.getTimezoneOffset(a);d=a.getTime()+c;a.setTime(d)}}):m?(this.get=function(c,a){return a["getUTC"+c]()},this.set=function(c,a,b){return a["setUTC"+c](b)}):(this.get=function(c,a){return a["get"+c]()},this.set=function(c,a,b){return a["set"+c](b)})},makeTime:function(m,p,q,h,a,b){if(this.useUTC){var d=
this.Date.UTC.apply(0,arguments);var e=this.getTimezoneOffset(d);d+=e;var k=this.getTimezoneOffset(d);e!==k?d+=k-e:e-36E5!==this.getTimezoneOffset(d-36E5)||c.isSafari||(d-=36E5)}else d=(new this.Date(m,p,w(q,1),w(h,0),w(a,0),w(b,0))).getTime();return d},timezoneOffsetFunction:function(){var m=this,p=this.options,q=t.moment;if(!this.useUTC)return function(c){return 6E4*(new Date(c)).getTimezoneOffset()};if(p.timezone){if(q)return function(c){return 6E4*-q.tz(c,p.timezone).utcOffset()};c.error(25)}return this.useUTC&&
p.getTimezoneOffset?function(c){return 6E4*p.getTimezoneOffset(c)}:function(){return 6E4*(m.timezoneOffset||0)}},dateFormat:function(m,p,q){if(!I(p)||isNaN(p))return c.defaultOptions.lang.invalidDate||"";m=w(m,"%Y-%m-%d %H:%M:%S");var h=this,a=new this.Date(p),b=this.get("Hours",a),d=this.get("Day",a),e=this.get("Date",a),k=this.get("Month",a),C=this.get("FullYear",a),z=c.defaultOptions.lang,r=z.weekdays,n=z.shortWeekdays,f=c.pad;a=G({a:n?n[d]:r[d].substr(0,3),A:r[d],d:f(e),e:f(e,2," "),w:d,b:z.shortMonths[k],
B:z.months[k],m:f(k+1),o:k+1,y:C.toString().substr(2,2),Y:C,H:f(b),k:b,I:f(b%12||12),l:b%12||12,M:f(h.get("Minutes",a)),p:12>b?"AM":"PM",P:12>b?"am":"pm",S:f(a.getSeconds()),L:f(Math.floor(p%1E3),3)},c.dateFormats);y(a,function(a,b){for(;-1!==m.indexOf("%"+b);)m=m.replace("%"+b,"function"===typeof a?a.call(h,p):a)});return q?m.substr(0,1).toUpperCase()+m.substr(1):m},resolveDTLFormat:function(c){return H(c,!0)?c:(c=x(c),{main:c[0],from:c[1],to:c[2]})},getTimeTicks:function(c,p,q,h){var a=this,b=[],
d={};var e=new a.Date(p);var k=c.unitRange,m=c.count||1,z;h=w(h,1);if(I(p)){a.set("Milliseconds",e,k>=F.second?0:m*Math.floor(a.get("Milliseconds",e)/m));k>=F.second&&a.set("Seconds",e,k>=F.minute?0:m*Math.floor(a.get("Seconds",e)/m));k>=F.minute&&a.set("Minutes",e,k>=F.hour?0:m*Math.floor(a.get("Minutes",e)/m));k>=F.hour&&a.set("Hours",e,k>=F.day?0:m*Math.floor(a.get("Hours",e)/m));k>=F.day&&a.set("Date",e,k>=F.month?1:Math.max(1,m*Math.floor(a.get("Date",e)/m)));if(k>=F.month){a.set("Month",e,k>=
F.year?0:m*Math.floor(a.get("Month",e)/m));var r=a.get("FullYear",e)}k>=F.year&&a.set("FullYear",e,r-r%m);k===F.week&&(r=a.get("Day",e),a.set("Date",e,a.get("Date",e)-r+h+(r<h?-7:0)));r=a.get("FullYear",e);h=a.get("Month",e);var n=a.get("Date",e),f=a.get("Hours",e);p=e.getTime();a.variableTimezone&&(z=q-p>4*F.month||a.getTimezoneOffset(p)!==a.getTimezoneOffset(q));p=e.getTime();for(e=1;p<q;)b.push(p),p=k===F.year?a.makeTime(r+e*m,0):k===F.month?a.makeTime(r,h+e*m):!z||k!==F.day&&k!==F.week?z&&k===
F.hour&&1<m?a.makeTime(r,h,n,f+e*m):p+k*m:a.makeTime(r,h,n+e*m*(k===F.day?1:7)),e++;b.push(p);k<=F.hour&&1E4>b.length&&b.forEach(function(b){0===b%18E5&&"000000000"===a.dateFormat("%H%M%S%L",b)&&(d[b]="day")})}b.info=G(c,{higherRanks:d,totalRange:k*m});return b}}});K(D,"parts/Options.js",[D["parts/Globals.js"]],function(c){var g=c.color,I=c.merge;c.defaultOptions={colors:"#7cb5ec #434348 #90ed7d #f7a35c #8085e9 #f15c80 #e4d354 #2b908f #f45b5b #91e8e1".split(" "),symbols:["circle","diamond","square",
"triangle","triangle-down"],lang:{loading:"Loading...",months:"January February March April May June July August September October November December".split(" "),shortMonths:"Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split(" "),weekdays:"Sunday Monday Tuesday Wednesday Thursday Friday Saturday".split(" "),decimalPoint:".",numericSymbols:"kMGTPE".split(""),resetZoom:"Reset zoom",resetZoomTitle:"Reset zoom level 1:1",thousandsSep:" "},global:{},time:c.Time.prototype.defaultOptions,chart:{styledMode:!1,
borderRadius:0,colorCount:10,defaultSeriesType:"line",ignoreHiddenSeries:!0,spacing:[10,10,15,10],resetZoomButton:{theme:{zIndex:6},position:{align:"right",x:-10,y:10}},width:null,height:null,borderColor:"#335cad",backgroundColor:"#ffffff",plotBorderColor:"#cccccc"},title:{text:"Chart title",align:"center",margin:15,widthAdjust:-44},subtitle:{text:"",align:"center",widthAdjust:-44},caption:{margin:15,text:"",align:"left",verticalAlign:"bottom"},plotOptions:{},labels:{style:{position:"absolute",color:"#333333"}},
legend:{enabled:!0,align:"center",alignColumns:!0,layout:"horizontal",labelFormatter:function(){return this.name},borderColor:"#999999",borderRadius:0,navigation:{activeColor:"#003399",inactiveColor:"#cccccc"},itemStyle:{color:"#333333",cursor:"pointer",fontSize:"12px",fontWeight:"bold",textOverflow:"ellipsis"},itemHoverStyle:{color:"#000000"},itemHiddenStyle:{color:"#cccccc"},shadow:!1,itemCheckboxStyle:{position:"absolute",width:"13px",height:"13px"},squareSymbol:!0,symbolPadding:5,verticalAlign:"bottom",
x:0,y:0,title:{style:{fontWeight:"bold"}}},loading:{labelStyle:{fontWeight:"bold",position:"relative",top:"45%"},style:{position:"absolute",backgroundColor:"#ffffff",opacity:.5,textAlign:"center"}},tooltip:{enabled:!0,animation:c.svg,borderRadius:3,dateTimeLabelFormats:{millisecond:"%A, %b %e, %H:%M:%S.%L",second:"%A, %b %e, %H:%M:%S",minute:"%A, %b %e, %H:%M",hour:"%A, %b %e, %H:%M",day:"%A, %b %e, %Y",week:"Week from %A, %b %e, %Y",month:"%B %Y",year:"%Y"},footerFormat:"",padding:8,snap:c.isTouchDevice?
25:10,headerFormat:'<span style="font-size: 10px">{point.key}</span><br/>',pointFormat:'<span style="color:{point.color}">\u25cf</span> {series.name}: <b>{point.y}</b><br/>',backgroundColor:g("#f7f7f7").setOpacity(.85).get(),borderWidth:1,shadow:!0,style:{color:"#333333",cursor:"default",fontSize:"12px",pointerEvents:"none",whiteSpace:"nowrap"}},credits:{enabled:!0,href:"https://www.highcharts.com?credits",position:{align:"right",x:-10,verticalAlign:"bottom",y:-5},style:{cursor:"pointer",color:"#999999",
fontSize:"9px"},text:"Highcharts.com"}};c.setOptions=function(g){c.defaultOptions=I(!0,c.defaultOptions,g);(g.time||g.global)&&c.time.update(I(c.defaultOptions.global,c.defaultOptions.time,g.global,g.time));return c.defaultOptions};c.getOptions=function(){return c.defaultOptions};c.defaultPlotOptions=c.defaultOptions.plotOptions;c.time=new c.Time(I(c.defaultOptions.global,c.defaultOptions.time));c.dateFormat=function(g,H,y){return c.time.dateFormat(g,H,y)};""});K(D,"parts/Tick.js",[D["parts/Globals.js"],
D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.destroyObjectProperties,H=g.extend,y=g.isNumber,w=g.pick,x=c.correctFloat,E=c.fireEvent,F=c.merge,t=c.deg2rad;c.Tick=function(c,p,q,h,a){this.axis=c;this.pos=p;this.type=q||"";this.isNewLabel=this.isNew=!0;this.parameters=a||{};this.tickmarkOffset=this.parameters.tickmarkOffset;this.options=this.parameters.options;q||h||this.addLabel()};c.Tick.prototype={addLabel:function(){var c=this,p=c.axis,q=p.options,h=p.chart,a=p.categories,b=p.names,
d=c.pos,e=w(c.options&&c.options.labels,q.labels),k=p.tickPositions,C=d===k[0],z=d===k[k.length-1];a=this.parameters.category||(a?w(a[d],b[d],d):d);var r=c.label;k=k.info;var n,f;if(p.isDatetimeAxis&&k){var l=h.time.resolveDTLFormat(q.dateTimeLabelFormats[!q.grid&&k.higherRanks[d]||k.unitName]);var v=l.main}c.isFirst=C;c.isLast=z;c.formatCtx={axis:p,chart:h,isFirst:C,isLast:z,dateTimeLabelFormat:v,tickPositionInfo:k,value:p.isLog?x(p.lin2log(a)):a,pos:d};q=p.labelFormatter.call(c.formatCtx,this.formatCtx);
if(f=l&&l.list)c.shortenLabel=function(){for(n=0;n<f.length;n++)if(r.attr({text:p.labelFormatter.call(H(c.formatCtx,{dateTimeLabelFormat:f[n]}))}),r.getBBox().width<p.getSlotWidth(c)-2*w(e.padding,5))return;r.attr({text:""})};if(I(r))r&&r.textStr!==q&&(!r.textWidth||e.style&&e.style.width||r.styles.width||r.css({width:null}),r.attr({text:q}),r.textPxLength=r.getBBox().width);else{if(c.label=r=I(q)&&e.enabled?h.renderer.text(q,0,0,e.useHTML).add(p.labelGroup):null)h.styledMode||r.css(F(e.style)),r.textPxLength=
r.getBBox().width;c.rotation=0}},getLabelSize:function(){return this.label?this.label.getBBox()[this.axis.horiz?"height":"width"]:0},handleOverflow:function(c){var m=this.axis,q=m.options.labels,h=c.x,a=m.chart.chartWidth,b=m.chart.spacing,d=w(m.labelLeft,Math.min(m.pos,b[3]));b=w(m.labelRight,Math.max(m.isRadial?0:m.pos+m.len,a-b[1]));var e=this.label,k=this.rotation,C={left:0,center:.5,right:1}[m.labelAlign||e.attr("align")],z=e.getBBox().width,r=m.getSlotWidth(this),n=r,f=1,l,v={};if(k||"justify"!==
w(q.overflow,"justify"))0>k&&h-C*z<d?l=Math.round(h/Math.cos(k*t)-d):0<k&&h+C*z>b&&(l=Math.round((a-h)/Math.cos(k*t)));else if(a=h+(1-C)*z,h-C*z<d?n=c.x+n*(1-C)-d:a>b&&(n=b-c.x+n*C,f=-1),n=Math.min(r,n),n<r&&"center"===m.labelAlign&&(c.x+=f*(r-n-C*(r-Math.min(z,n)))),z>n||m.autoRotation&&(e.styles||{}).width)l=n;l&&(this.shortenLabel?this.shortenLabel():(v.width=Math.floor(l),(q.style||{}).textOverflow||(v.textOverflow="ellipsis"),e.css(v)))},getPosition:function(m,p,q,h){var a=this.axis,b=a.chart,
d=h&&b.oldChartHeight||b.chartHeight;m={x:m?c.correctFloat(a.translate(p+q,null,null,h)+a.transB):a.left+a.offset+(a.opposite?(h&&b.oldChartWidth||b.chartWidth)-a.right-a.left:0),y:m?d-a.bottom+a.offset-(a.opposite?a.height:0):c.correctFloat(d-a.translate(p+q,null,null,h)-a.transB)};m.y=Math.max(Math.min(m.y,1E5),-1E5);E(this,"afterGetPosition",{pos:m});return m},getLabelPosition:function(c,p,q,h,a,b,d,e){var k=this.axis,m=k.transA,z=k.isLinked&&k.linkedParent?k.linkedParent.reversed:k.reversed,r=
k.staggerLines,n=k.tickRotCorr||{x:0,y:0},f=a.y,l=h||k.reserveSpaceDefault?0:-k.labelOffset*("center"===k.labelAlign?.5:1),v={};I(f)||(f=0===k.side?q.rotation?-8:-q.getBBox().height:2===k.side?n.y+8:Math.cos(q.rotation*t)*(n.y-q.getBBox(!1,0).height/2));c=c+a.x+l+n.x-(b&&h?b*m*(z?-1:1):0);p=p+f-(b&&!h?b*m*(z?1:-1):0);r&&(q=d/(e||1)%r,k.opposite&&(q=r-q-1),p+=k.labelOffset/r*q);v.x=c;v.y=Math.round(p);E(this,"afterGetLabelPosition",{pos:v,tickmarkOffset:b,index:d});return v},getMarkPath:function(c,
p,q,h,a,b){return b.crispLine(["M",c,p,"L",c+(a?0:-q),p+(a?q:0)],h)},renderGridLine:function(c,p,q){var h=this.axis,a=h.options,b=this.gridLine,d={},e=this.pos,k=this.type,m=w(this.tickmarkOffset,h.tickmarkOffset),z=h.chart.renderer,r=k?k+"Grid":"grid",n=a[r+"LineWidth"],f=a[r+"LineColor"];a=a[r+"LineDashStyle"];b||(h.chart.styledMode||(d.stroke=f,d["stroke-width"]=n,a&&(d.dashstyle=a)),k||(d.zIndex=1),c&&(p=0),this.gridLine=b=z.path().attr(d).addClass("highcharts-"+(k?k+"-":"")+"grid-line").add(h.gridGroup));
if(b&&(q=h.getPlotLinePath({value:e+m,lineWidth:b.strokeWidth()*q,force:"pass",old:c})))b[c||this.isNew?"attr":"animate"]({d:q,opacity:p})},renderMark:function(c,p,q){var h=this.axis,a=h.options,b=h.chart.renderer,d=this.type,e=d?d+"Tick":"tick",k=h.tickSize(e),m=this.mark,z=!m,r=c.x;c=c.y;var n=w(a[e+"Width"],!d&&h.isXAxis?1:0);a=a[e+"Color"];k&&(h.opposite&&(k[0]=-k[0]),z&&(this.mark=m=b.path().addClass("highcharts-"+(d?d+"-":"")+"tick").add(h.axisGroup),h.chart.styledMode||m.attr({stroke:a,"stroke-width":n})),
m[z?"attr":"animate"]({d:this.getMarkPath(r,c,k[0],m.strokeWidth()*q,h.horiz,b),opacity:p}))},renderLabel:function(c,p,q,h){var a=this.axis,b=a.horiz,d=a.options,e=this.label,k=d.labels,m=k.step;a=w(this.tickmarkOffset,a.tickmarkOffset);var z=!0,r=c.x;c=c.y;e&&y(r)&&(e.xy=c=this.getLabelPosition(r,c,e,b,k,a,h,m),this.isFirst&&!this.isLast&&!w(d.showFirstLabel,1)||this.isLast&&!this.isFirst&&!w(d.showLastLabel,1)?z=!1:!b||k.step||k.rotation||p||0===q||this.handleOverflow(c),m&&h%m&&(z=!1),z&&y(c.y)?
(c.opacity=q,e[this.isNewLabel?"attr":"animate"](c),this.isNewLabel=!1):(e.attr("y",-9999),this.isNewLabel=!0))},render:function(m,p,q){var h=this.axis,a=h.horiz,b=this.pos,d=w(this.tickmarkOffset,h.tickmarkOffset);b=this.getPosition(a,b,d,p);d=b.x;var e=b.y;h=a&&d===h.pos+h.len||!a&&e===h.pos?-1:1;q=w(q,1);this.isActive=!0;this.renderGridLine(p,q,h);this.renderMark(b,q,h);this.renderLabel(b,p,q,m);this.isNew=!1;c.fireEvent(this,"afterRender")},destroy:function(){G(this,this.axis)}}});K(D,"parts/Axis.js",
[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.arrayMax,G=g.arrayMin,H=g.defined,y=g.destroyObjectProperties,w=g.extend,x=g.isArray,E=g.isNumber,F=g.isString,t=g.objectEach,m=g.pick,p=g.splat,q=g.syncTimeout,h=c.addEvent,a=c.animObject,b=c.color,d=c.correctFloat,e=c.defaultOptions,k=c.deg2rad,C=c.fireEvent,z=c.format,r=c.getMagnitude,n=c.merge,f=c.normalizeTickInterval,l=c.removeEvent,v=c.seriesTypes,B=c.Tick;g=function(){this.init.apply(this,arguments)};w(g.prototype,{defaultOptions:{dateTimeLabelFormats:{millisecond:{main:"%H:%M:%S.%L",
range:!1},second:{main:"%H:%M:%S",range:!1},minute:{main:"%H:%M",range:!1},hour:{main:"%H:%M",range:!1},day:{main:"%e. %b"},week:{main:"%e. %b"},month:{main:"%b '%y"},year:{main:"%Y"}},endOnTick:!1,labels:{enabled:!0,indentation:10,x:0,style:{color:"#666666",cursor:"default",fontSize:"11px"}},maxPadding:.01,minorTickLength:2,minorTickPosition:"outside",minPadding:.01,showEmpty:!0,startOfWeek:1,startOnTick:!1,tickLength:10,tickPixelInterval:100,tickmarkPlacement:"between",tickPosition:"outside",title:{align:"middle",
style:{color:"#666666"}},type:"linear",minorGridLineColor:"#f2f2f2",minorGridLineWidth:1,minorTickColor:"#999999",lineColor:"#ccd6eb",lineWidth:1,gridLineColor:"#e6e6e6",tickColor:"#ccd6eb"},defaultYAxisOptions:{endOnTick:!0,maxPadding:.05,minPadding:.05,tickPixelInterval:72,showLastLabel:!0,labels:{x:-8},startOnTick:!0,title:{rotation:270,text:"Values"},stackLabels:{allowOverlap:!1,enabled:!1,crop:!0,overflow:"justify",formatter:function(){return c.numberFormat(this.total,-1)},style:{color:"#000000",
fontSize:"11px",fontWeight:"bold",textOutline:"1px contrast"}},gridLineWidth:1,lineWidth:0},defaultLeftAxisOptions:{labels:{x:-15},title:{rotation:270}},defaultRightAxisOptions:{labels:{x:15},title:{rotation:90}},defaultBottomAxisOptions:{labels:{autoRotation:[-45],x:0},margin:15,title:{rotation:0}},defaultTopAxisOptions:{labels:{autoRotation:[-45],x:0},margin:15,title:{rotation:0}},init:function(a,b){var f=b.isX,l=this;l.chart=a;l.horiz=a.inverted&&!l.isZAxis?!f:f;l.isXAxis=f;l.coll=l.coll||(f?"xAxis":
"yAxis");C(this,"init",{userOptions:b});l.opposite=b.opposite;l.side=b.side||(l.horiz?l.opposite?0:2:l.opposite?1:3);l.setOptions(b);var u=this.options,A=u.type;l.labelFormatter=u.labels.formatter||l.defaultLabelFormatter;l.userOptions=b;l.minPixelPadding=0;l.reversed=u.reversed;l.visible=!1!==u.visible;l.zoomEnabled=!1!==u.zoomEnabled;l.hasNames="category"===A||!0===u.categories;l.categories=u.categories||l.hasNames;l.names||(l.names=[],l.names.keys={});l.plotLinesAndBandsGroups={};l.isLog="logarithmic"===
A;l.isDatetimeAxis="datetime"===A;l.positiveValuesOnly=l.isLog&&!l.allowNegativeLog;l.isLinked=H(u.linkedTo);l.ticks={};l.labelEdge=[];l.minorTicks={};l.plotLinesAndBands=[];l.alternateBands={};l.len=0;l.minRange=l.userMinRange=u.minRange||u.maxZoom;l.range=u.range;l.offset=u.offset||0;l.stacks={};l.oldStacks={};l.stacksTouched=0;l.max=null;l.min=null;l.crosshair=m(u.crosshair,p(a.options.tooltip.crosshairs)[f?0:1],!1);b=l.options.events;-1===a.axes.indexOf(l)&&(f?a.axes.splice(a.xAxis.length,0,l):
a.axes.push(l),a[l.coll].push(l));l.series=l.series||[];a.inverted&&!l.isZAxis&&f&&void 0===l.reversed&&(l.reversed=!0);t(b,function(a,b){c.isFunction(a)&&h(l,b,a)});l.lin2log=u.linearToLogConverter||l.lin2log;l.isLog&&(l.val2lin=l.log2lin,l.lin2val=l.lin2log);C(this,"afterInit")},setOptions:function(a){this.options=n(this.defaultOptions,"yAxis"===this.coll&&this.defaultYAxisOptions,[this.defaultTopAxisOptions,this.defaultRightAxisOptions,this.defaultBottomAxisOptions,this.defaultLeftAxisOptions][this.side],
n(e[this.coll],a));C(this,"afterSetOptions",{userOptions:a})},defaultLabelFormatter:function(){var a=this.axis,b=this.value,f=a.chart.time,l=a.categories,d=this.dateTimeLabelFormat,k=e.lang,n=k.numericSymbols;k=k.numericSymbolMagnitude||1E3;var v=n&&n.length,B=a.options.labels.format;a=a.isLog?Math.abs(b):a.tickInterval;if(B)var h=z(B,this,f);else if(l)h=b;else if(d)h=f.dateFormat(d,b);else if(v&&1E3<=a)for(;v--&&void 0===h;)f=Math.pow(k,v+1),a>=f&&0===10*b%f&&null!==n[v]&&0!==b&&(h=c.numberFormat(b/
f,-1)+n[v]);void 0===h&&(h=1E4<=Math.abs(b)?c.numberFormat(b,-1):c.numberFormat(b,-1,void 0,""));return h},getSeriesExtremes:function(){var a=this,b=a.chart,f;C(this,"getSeriesExtremes",null,function(){a.hasVisibleSeries=!1;a.dataMin=a.dataMax=a.threshold=null;a.softThreshold=!a.isXAxis;a.buildStacks&&a.buildStacks();a.series.forEach(function(l){if(l.visible||!b.options.chart.ignoreHiddenSeries){var u=l.options,A=u.threshold;a.hasVisibleSeries=!0;a.positiveValuesOnly&&0>=A&&(A=null);if(a.isXAxis){if(u=
l.xData,u.length){f=l.getXExtremes(u);var d=f.min;var e=f.max;E(d)||d instanceof Date||(u=u.filter(E),f=l.getXExtremes(u),d=f.min,e=f.max);u.length&&(a.dataMin=Math.min(m(a.dataMin,d),d),a.dataMax=Math.max(m(a.dataMax,e),e))}}else if(l.getExtremes(),e=l.dataMax,d=l.dataMin,H(d)&&H(e)&&(a.dataMin=Math.min(m(a.dataMin,d),d),a.dataMax=Math.max(m(a.dataMax,e),e)),H(A)&&(a.threshold=A),!u.softThreshold||a.positiveValuesOnly)a.softThreshold=!1}})});C(this,"afterGetSeriesExtremes")},translate:function(a,
b,f,l,d,e){var u=this.linkedParent||this,A=1,k=0,n=l?u.oldTransA:u.transA;l=l?u.oldMin:u.min;var c=u.minPixelPadding;d=(u.isOrdinal||u.isBroken||u.isLog&&d)&&u.lin2val;n||(n=u.transA);f&&(A*=-1,k=u.len);u.reversed&&(A*=-1,k-=A*(u.sector||u.len));b?(a=(a*A+k-c)/n+l,d&&(a=u.lin2val(a))):(d&&(a=u.val2lin(a)),a=E(l)?A*(a-l)*n+k+A*c+(E(e)?n*e:0):void 0);return a},toPixels:function(a,b){return this.translate(a,!1,!this.horiz,null,!0)+(b?0:this.pos)},toValue:function(a,b){return this.translate(a-(b?0:this.pos),
!0,!this.horiz,null,!0)},getPlotLinePath:function(a){var b=this,f=b.chart,l=b.left,d=b.top,A=a.old,e=a.value,k=a.translatedValue,n=a.lineWidth,c=a.force,v,B,h,r,z=A&&f.oldChartHeight||f.chartHeight,p=A&&f.oldChartWidth||f.chartWidth,q,g=b.transB,t=function(a,b,f){if("pass"!==c&&a<b||a>f)c?a=Math.min(Math.max(b,a),f):q=!0;return a};a={value:e,lineWidth:n,old:A,force:c,acrossPanes:a.acrossPanes,translatedValue:k};C(this,"getPlotLinePath",a,function(a){k=m(k,b.translate(e,null,null,A));k=Math.min(Math.max(-1E5,
k),1E5);v=h=Math.round(k+g);B=r=Math.round(z-k-g);E(k)?b.horiz?(B=d,r=z-b.bottom,v=h=t(v,l,l+b.width)):(v=l,h=p-b.right,B=r=t(B,d,d+b.height)):(q=!0,c=!1);a.path=q&&!c?null:f.renderer.crispLine(["M",v,B,"L",h,r],n||1)});return a.path},getLinearTickPositions:function(a,b,f){var l=d(Math.floor(b/a)*a);f=d(Math.ceil(f/a)*a);var u=[],A;d(l+a)===l&&(A=20);if(this.single)return[b];for(b=l;b<=f;){u.push(b);b=d(b+a,A);if(b===e)break;var e=b}return u},getMinorTickInterval:function(){var a=this.options;return!0===
a.minorTicks?m(a.minorTickInterval,"auto"):!1===a.minorTicks?null:a.minorTickInterval},getMinorTickPositions:function(){var a=this,b=a.options,f=a.tickPositions,l=a.minorTickInterval,d=[],e=a.pointRangePadding||0,k=a.min-e;e=a.max+e;var n=e-k;if(n&&n/l<a.len/3)if(a.isLog)this.paddedTicks.forEach(function(b,f,u){f&&d.push.apply(d,a.getLogTickPositions(l,u[f-1],u[f],!0))});else if(a.isDatetimeAxis&&"auto"===this.getMinorTickInterval())d=d.concat(a.getTimeTicks(a.normalizeTimeTickInterval(l),k,e,b.startOfWeek));
else for(b=k+(f[0]-k)%l;b<=e&&b!==d[0];b+=l)d.push(b);0!==d.length&&a.trimTicks(d);return d},adjustForMinRange:function(){var a=this.options,b=this.min,f=this.max,l,d,e,k,n;this.isXAxis&&void 0===this.minRange&&!this.isLog&&(H(a.min)||H(a.max)?this.minRange=null:(this.series.forEach(function(a){k=a.xData;for(d=n=a.xIncrement?1:k.length-1;0<d;d--)if(e=k[d]-k[d-1],void 0===l||e<l)l=e}),this.minRange=Math.min(5*l,this.dataMax-this.dataMin)));if(f-b<this.minRange){var c=this.dataMax-this.dataMin>=this.minRange;
var v=this.minRange;var B=(v-f+b)/2;B=[b-B,m(a.min,b-B)];c&&(B[2]=this.isLog?this.log2lin(this.dataMin):this.dataMin);b=I(B);f=[b+v,m(a.max,b+v)];c&&(f[2]=this.isLog?this.log2lin(this.dataMax):this.dataMax);f=G(f);f-b<v&&(B[0]=f-v,B[1]=m(a.min,f-v),b=I(B))}this.min=b;this.max=f},getClosest:function(){var a;this.categories?a=1:this.series.forEach(function(b){var f=b.closestPointRange,l=b.visible||!b.chart.options.chart.ignoreHiddenSeries;!b.noSharedTooltip&&H(f)&&l&&(a=H(a)?Math.min(a,f):f)});return a},
nameToX:function(a){var b=x(this.categories),f=b?this.categories:this.names,l=a.options.x;a.series.requireSorting=!1;H(l)||(l=!1===this.options.uniqueNames?a.series.autoIncrement():b?f.indexOf(a.name):m(f.keys[a.name],-1));if(-1===l){if(!b)var d=f.length}else d=l;void 0!==d&&(this.names[d]=a.name,this.names.keys[a.name]=d);return d},updateNames:function(){var a=this,b=this.names;0<b.length&&(Object.keys(b.keys).forEach(function(a){delete b.keys[a]}),b.length=0,this.minRange=this.userMinRange,(this.series||
[]).forEach(function(b){b.xIncrement=null;if(!b.points||b.isDirtyData)a.max=Math.max(a.max,b.xData.length-1),b.processData(),b.generatePoints();b.data.forEach(function(f,l){if(f&&f.options&&void 0!==f.name){var u=a.nameToX(f);void 0!==u&&u!==f.x&&(f.x=u,b.xData[l]=u)}})}))},setAxisTranslation:function(a){var b=this,f=b.max-b.min,l=b.axisPointRange||0,d=0,e=0,k=b.linkedParent,A=!!b.categories,n=b.transA,c=b.isXAxis;if(c||A||l){var B=b.getClosest();k?(d=k.minPointOffset,e=k.pointRangePadding):b.series.forEach(function(a){var f=
A?1:c?m(a.options.pointRange,B,0):b.axisPointRange||0,u=a.options.pointPlacement;l=Math.max(l,f);if(!b.single||A)a=v.xrange&&a instanceof v.xrange?!c:c,d=Math.max(d,a&&F(u)?0:f/2),e=Math.max(e,a&&"on"===u?0:f)});k=b.ordinalSlope&&B?b.ordinalSlope/B:1;b.minPointOffset=d*=k;b.pointRangePadding=e*=k;b.pointRange=Math.min(l,b.single&&A?1:f);c&&(b.closestPointRange=B)}a&&(b.oldTransA=n);b.translationSlope=b.transA=n=b.staticScale||b.len/(f+e||1);b.transB=b.horiz?b.left:b.bottom;b.minPixelPadding=n*d;C(this,
"afterSetAxisTranslation")},minFromRange:function(){return this.max-this.range},setTickInterval:function(a){var b=this,l=b.chart,e=b.options,k=b.isLog,A=b.isDatetimeAxis,n=b.isXAxis,v=b.isLinked,B=e.maxPadding,h=e.minPadding,z=e.tickInterval,p=e.tickPixelInterval,q=b.categories,g=E(b.threshold)?b.threshold:null,t=b.softThreshold;A||q||v||this.getTickAmount();var x=m(b.userMin,e.min);var w=m(b.userMax,e.max);if(v){b.linkedParent=l[b.coll][e.linkedTo];var y=b.linkedParent.getExtremes();b.min=m(y.min,
y.dataMin);b.max=m(y.max,y.dataMax);e.type!==b.linkedParent.options.type&&c.error(11,1,l)}else{if(!t&&H(g))if(b.dataMin>=g)y=g,h=0;else if(b.dataMax<=g){var F=g;B=0}b.min=m(x,y,b.dataMin);b.max=m(w,F,b.dataMax)}k&&(b.positiveValuesOnly&&!a&&0>=Math.min(b.min,m(b.dataMin,b.min))&&c.error(10,1,l),b.min=d(b.log2lin(b.min),16),b.max=d(b.log2lin(b.max),16));b.range&&H(b.max)&&(b.userMin=b.min=x=Math.max(b.dataMin,b.minFromRange()),b.userMax=w=b.max,b.range=null);C(b,"foundExtremes");b.beforePadding&&b.beforePadding();
b.adjustForMinRange();!(q||b.axisPointRange||b.usePercentage||v)&&H(b.min)&&H(b.max)&&(l=b.max-b.min)&&(!H(x)&&h&&(b.min-=l*h),!H(w)&&B&&(b.max+=l*B));E(e.softMin)&&!E(b.userMin)&&e.softMin<b.min&&(b.min=x=e.softMin);E(e.softMax)&&!E(b.userMax)&&e.softMax>b.max&&(b.max=w=e.softMax);E(e.floor)&&(b.min=Math.min(Math.max(b.min,e.floor),Number.MAX_VALUE));E(e.ceiling)&&(b.max=Math.max(Math.min(b.max,e.ceiling),m(b.userMax,-Number.MAX_VALUE)));t&&H(b.dataMin)&&(g=g||0,!H(x)&&b.min<g&&b.dataMin>=g?b.min=
b.options.minRange?Math.min(g,b.max-b.minRange):g:!H(w)&&b.max>g&&b.dataMax<=g&&(b.max=b.options.minRange?Math.max(g,b.min+b.minRange):g));b.tickInterval=b.min===b.max||void 0===b.min||void 0===b.max?1:v&&!z&&p===b.linkedParent.options.tickPixelInterval?z=b.linkedParent.tickInterval:m(z,this.tickAmount?(b.max-b.min)/Math.max(this.tickAmount-1,1):void 0,q?1:(b.max-b.min)*p/Math.max(b.len,p));n&&!a&&b.series.forEach(function(a){a.processData(b.min!==b.oldMin||b.max!==b.oldMax)});b.setAxisTranslation(!0);
b.beforeSetTickPositions&&b.beforeSetTickPositions();b.postProcessTickInterval&&(b.tickInterval=b.postProcessTickInterval(b.tickInterval));b.pointRange&&!z&&(b.tickInterval=Math.max(b.pointRange,b.tickInterval));a=m(e.minTickInterval,b.isDatetimeAxis&&b.closestPointRange);!z&&b.tickInterval<a&&(b.tickInterval=a);A||k||z||(b.tickInterval=f(b.tickInterval,null,r(b.tickInterval),m(e.allowDecimals,!(.5<b.tickInterval&&5>b.tickInterval&&1E3<b.max&&9999>b.max)),!!this.tickAmount));this.tickAmount||(b.tickInterval=
b.unsquish());this.setTickPositions()},setTickPositions:function(){var a=this.options,b=a.tickPositions;var f=this.getMinorTickInterval();var l=a.tickPositioner,d=a.startOnTick,e=a.endOnTick;this.tickmarkOffset=this.categories&&"between"===a.tickmarkPlacement&&1===this.tickInterval?.5:0;this.minorTickInterval="auto"===f&&this.tickInterval?this.tickInterval/5:f;this.single=this.min===this.max&&H(this.min)&&!this.tickAmount&&(parseInt(this.min,10)===this.min||!1!==a.allowDecimals);this.tickPositions=
f=b&&b.slice();!f&&(!this.ordinalPositions&&(this.max-this.min)/this.tickInterval>Math.max(2*this.len,200)?(f=[this.min,this.max],c.error(19,!1,this.chart)):f=this.isDatetimeAxis?this.getTimeTicks(this.normalizeTimeTickInterval(this.tickInterval,a.units),this.min,this.max,a.startOfWeek,this.ordinalPositions,this.closestPointRange,!0):this.isLog?this.getLogTickPositions(this.tickInterval,this.min,this.max):this.getLinearTickPositions(this.tickInterval,this.min,this.max),f.length>this.len&&(f=[f[0],
f.pop()],f[0]===f[1]&&(f.length=1)),this.tickPositions=f,l&&(l=l.apply(this,[this.min,this.max])))&&(this.tickPositions=f=l);this.paddedTicks=f.slice(0);this.trimTicks(f,d,e);this.isLinked||(this.single&&2>f.length&&!this.categories&&(this.min-=.5,this.max+=.5),b||l||this.adjustTickAmount());C(this,"afterSetTickPositions")},trimTicks:function(a,b,f){var l=a[0],d=a[a.length-1],e=this.minPointOffset||0;C(this,"trimTicks");if(!this.isLinked){if(b&&-Infinity!==l)this.min=l;else for(;this.min-e>a[0];)a.shift();
if(f)this.max=d;else for(;this.max+e<a[a.length-1];)a.pop();0===a.length&&H(l)&&!this.options.tickPositions&&a.push((d+l)/2)}},alignToOthers:function(){var a={},b,f=this.options;!1===this.chart.options.chart.alignTicks||!1===f.alignTicks||!1===f.startOnTick||!1===f.endOnTick||this.isLog||this.chart[this.coll].forEach(function(f){var l=f.options;l=[f.horiz?l.left:l.top,l.width,l.height,l.pane].join();f.series.length&&(a[l]?b=!0:a[l]=1)});return b},getTickAmount:function(){var a=this.options,b=a.tickAmount,
f=a.tickPixelInterval;!H(a.tickInterval)&&this.len<f&&!this.isRadial&&!this.isLog&&a.startOnTick&&a.endOnTick&&(b=2);!b&&this.alignToOthers()&&(b=Math.ceil(this.len/f)+1);4>b&&(this.finalTickAmt=b,b=5);this.tickAmount=b},adjustTickAmount:function(){var a=this.options,b=this.tickInterval,f=this.tickPositions,l=this.tickAmount,e=this.finalTickAmt,k=f&&f.length,n=m(this.threshold,this.softThreshold?0:null),c;if(this.hasData()){if(k<l){for(c=this.min;f.length<l;)f.length%2||c===n?f.push(d(f[f.length-
1]+b)):f.unshift(d(f[0]-b));this.transA*=(k-1)/(l-1);this.min=a.startOnTick?f[0]:Math.min(this.min,f[0]);this.max=a.endOnTick?f[f.length-1]:Math.max(this.max,f[f.length-1])}else k>l&&(this.tickInterval*=2,this.setTickPositions());if(H(e)){for(b=a=f.length;b--;)(3===e&&1===b%2||2>=e&&0<b&&b<a-1)&&f.splice(b,1);this.finalTickAmt=void 0}}},setScale:function(){var a=this.series.some(function(a){return a.isDirtyData||a.isDirty||a.xAxis&&a.xAxis.isDirty}),b;this.oldMin=this.min;this.oldMax=this.max;this.oldAxisLength=
this.len;this.setAxisSize();(b=this.len!==this.oldAxisLength)||a||this.isLinked||this.forceRedraw||this.userMin!==this.oldUserMin||this.userMax!==this.oldUserMax||this.alignToOthers()?(this.resetStacks&&this.resetStacks(),this.forceRedraw=!1,this.getSeriesExtremes(),this.setTickInterval(),this.oldUserMin=this.userMin,this.oldUserMax=this.userMax,this.isDirty||(this.isDirty=b||this.min!==this.oldMin||this.max!==this.oldMax)):this.cleanStacks&&this.cleanStacks();C(this,"afterSetScale")},setExtremes:function(a,
b,f,l,d){var e=this,k=e.chart;f=m(f,!0);e.series.forEach(function(a){delete a.kdTree});d=w(d,{min:a,max:b});C(e,"setExtremes",d,function(){e.userMin=a;e.userMax=b;e.eventArgs=d;f&&k.redraw(l)})},zoom:function(a,b){var f=this.dataMin,l=this.dataMax,d=this.options,e=Math.min(f,m(d.min,f)),k=Math.max(l,m(d.max,l));a={newMin:a,newMax:b};C(this,"zoom",a,function(a){var b=a.newMin,d=a.newMax;if(b!==this.min||d!==this.max)this.allowZoomOutside||(H(f)&&(b<e&&(b=e),b>k&&(b=k)),H(l)&&(d<e&&(d=e),d>k&&(d=k))),
this.displayBtn=void 0!==b||void 0!==d,this.setExtremes(b,d,!1,void 0,{trigger:"zoom"});a.zoomed=!0});return a.zoomed},setAxisSize:function(){var a=this.chart,b=this.options,f=b.offsets||[0,0,0,0],l=this.horiz,d=this.width=Math.round(c.relativeLength(m(b.width,a.plotWidth-f[3]+f[1]),a.plotWidth)),e=this.height=Math.round(c.relativeLength(m(b.height,a.plotHeight-f[0]+f[2]),a.plotHeight)),k=this.top=Math.round(c.relativeLength(m(b.top,a.plotTop+f[0]),a.plotHeight,a.plotTop));b=this.left=Math.round(c.relativeLength(m(b.left,
a.plotLeft+f[3]),a.plotWidth,a.plotLeft));this.bottom=a.chartHeight-e-k;this.right=a.chartWidth-d-b;this.len=Math.max(l?d:e,0);this.pos=l?b:k},getExtremes:function(){var a=this.isLog;return{min:a?d(this.lin2log(this.min)):this.min,max:a?d(this.lin2log(this.max)):this.max,dataMin:this.dataMin,dataMax:this.dataMax,userMin:this.userMin,userMax:this.userMax}},getThreshold:function(a){var b=this.isLog,f=b?this.lin2log(this.min):this.min;b=b?this.lin2log(this.max):this.max;null===a||-Infinity===a?a=f:Infinity===
a?a=b:f>a?a=f:b<a&&(a=b);return this.translate(a,0,1,0,1)},autoLabelAlign:function(a){var b=(m(a,0)-90*this.side+720)%360;a={align:"center"};C(this,"autoLabelAlign",a,function(a){15<b&&165>b?a.align="right":195<b&&345>b&&(a.align="left")});return a.align},tickSize:function(a){var b=this.options,f=b[a+"Length"],l=m(b[a+"Width"],"tick"===a&&this.isXAxis&&!this.categories?1:0);if(l&&f){"inside"===b[a+"Position"]&&(f=-f);var d=[f,l]}a={tickSize:d};C(this,"afterTickSize",a);return a.tickSize},labelMetrics:function(){var a=
this.tickPositions&&this.tickPositions[0]||0;return this.chart.renderer.fontMetrics(this.options.labels.style&&this.options.labels.style.fontSize,this.ticks[a]&&this.ticks[a].label)},unsquish:function(){var a=this.options.labels,b=this.horiz,f=this.tickInterval,l=f,e=this.len/(((this.categories?1:0)+this.max-this.min)/f),n,c=a.rotation,v=this.labelMetrics(),B,h=Number.MAX_VALUE,r,C=this.max-this.min,z=function(a){var b=a/(e||1);b=1<b?Math.ceil(b):1;b*f>C&&Infinity!==a&&Infinity!==e&&C&&(b=Math.ceil(C/
f));return d(b*f)};b?(r=!a.staggerLines&&!a.step&&(H(c)?[c]:e<m(a.autoRotationLimit,80)&&a.autoRotation))&&r.forEach(function(a){if(a===c||a&&-90<=a&&90>=a){B=z(Math.abs(v.h/Math.sin(k*a)));var b=B+Math.abs(a/360);b<h&&(h=b,n=a,l=B)}}):a.step||(l=z(v.h));this.autoRotation=r;this.labelRotation=m(n,c);return l},getSlotWidth:function(a){var b=this.chart,f=this.horiz,l=this.options.labels,d=Math.max(this.tickPositions.length-(this.categories?0:1),1),e=b.margin[3];return a&&a.slotWidth||f&&2>(l.step||
0)&&!l.rotation&&(this.staggerLines||1)*this.len/d||!f&&(l.style&&parseInt(l.style.width,10)||e&&e-b.spacing[3]||.33*b.chartWidth)},renderUnsquish:function(){var a=this.chart,b=a.renderer,f=this.tickPositions,l=this.ticks,d=this.options.labels,e=d&&d.style||{},k=this.horiz,n=this.getSlotWidth(),c=Math.max(1,Math.round(n-2*(d.padding||5))),v={},B=this.labelMetrics(),h=d.style&&d.style.textOverflow,r=0;F(d.rotation)||(v.rotation=d.rotation||0);f.forEach(function(a){(a=l[a])&&a.label&&a.label.textPxLength>
r&&(r=a.label.textPxLength)});this.maxLabelLength=r;if(this.autoRotation)r>c&&r>B.h?v.rotation=this.labelRotation:this.labelRotation=0;else if(n){var m=c;if(!h){var C="clip";for(c=f.length;!k&&c--;){var z=f[c];if(z=l[z].label)z.styles&&"ellipsis"===z.styles.textOverflow?z.css({textOverflow:"clip"}):z.textPxLength>n&&z.css({width:n+"px"}),z.getBBox().height>this.len/f.length-(B.h-B.f)&&(z.specificTextOverflow="ellipsis")}}}v.rotation&&(m=r>.5*a.chartHeight?.33*a.chartHeight:r,h||(C="ellipsis"));if(this.labelAlign=
d.align||this.autoLabelAlign(this.labelRotation))v.align=this.labelAlign;f.forEach(function(a){var b=(a=l[a])&&a.label,f=e.width,d={};b&&(b.attr(v),a.shortenLabel?a.shortenLabel():m&&!f&&"nowrap"!==e.whiteSpace&&(m<b.textPxLength||"SPAN"===b.element.tagName)?(d.width=m,h||(d.textOverflow=b.specificTextOverflow||C),b.css(d)):b.styles&&b.styles.width&&!d.width&&!f&&b.css({width:null}),delete b.specificTextOverflow,a.rotation=v.rotation)},this);this.tickRotCorr=b.rotCorr(B.b,this.labelRotation||0,0!==
this.side)},hasData:function(){return this.series.some(function(a){return a.hasData()})||this.options.showEmpty&&H(this.min)&&H(this.max)},addTitle:function(a){var b=this.chart.renderer,f=this.horiz,l=this.opposite,d=this.options.title,e,k=this.chart.styledMode;this.axisTitle||((e=d.textAlign)||(e=(f?{low:"left",middle:"center",high:"right"}:{low:l?"right":"left",middle:"center",high:l?"left":"right"})[d.align]),this.axisTitle=b.text(d.text,0,0,d.useHTML).attr({zIndex:7,rotation:d.rotation||0,align:e}).addClass("highcharts-axis-title"),
k||this.axisTitle.css(n(d.style)),this.axisTitle.add(this.axisGroup),this.axisTitle.isNew=!0);k||d.style.width||this.isRadial||this.axisTitle.css({width:this.len});this.axisTitle[a?"show":"hide"](a)},generateTick:function(a){var b=this.ticks;b[a]?b[a].addLabel():b[a]=new B(this,a)},getOffset:function(){var a=this,b=a.chart,f=b.renderer,l=a.options,d=a.tickPositions,e=a.ticks,k=a.horiz,n=a.side,c=b.inverted&&!a.isZAxis?[1,0,3,2][n]:n,v,B=0,h=0,r=l.title,z=l.labels,p=0,g=b.axisOffset;b=b.clipOffset;
var q=[-1,1,1,-1][n],x=l.className,w=a.axisParent;var E=a.hasData();a.showAxis=v=E||m(l.showEmpty,!0);a.staggerLines=a.horiz&&z.staggerLines;a.axisGroup||(a.gridGroup=f.g("grid").attr({zIndex:l.gridZIndex||1}).addClass("highcharts-"+this.coll.toLowerCase()+"-grid "+(x||"")).add(w),a.axisGroup=f.g("axis").attr({zIndex:l.zIndex||2}).addClass("highcharts-"+this.coll.toLowerCase()+" "+(x||"")).add(w),a.labelGroup=f.g("axis-labels").attr({zIndex:z.zIndex||7}).addClass("highcharts-"+a.coll.toLowerCase()+
"-labels "+(x||"")).add(w));E||a.isLinked?(d.forEach(function(b,f){a.generateTick(b,f)}),a.renderUnsquish(),a.reserveSpaceDefault=0===n||2===n||{1:"left",3:"right"}[n]===a.labelAlign,m(z.reserveSpace,"center"===a.labelAlign?!0:null,a.reserveSpaceDefault)&&d.forEach(function(a){p=Math.max(e[a].getLabelSize(),p)}),a.staggerLines&&(p*=a.staggerLines),a.labelOffset=p*(a.opposite?-1:1)):t(e,function(a,b){a.destroy();delete e[b]});if(r&&r.text&&!1!==r.enabled&&(a.addTitle(v),v&&!1!==r.reserveSpace)){a.titleOffset=
B=a.axisTitle.getBBox()[k?"height":"width"];var y=r.offset;h=H(y)?0:m(r.margin,k?5:10)}a.renderLine();a.offset=q*m(l.offset,g[n]?g[n]+(l.margin||0):0);a.tickRotCorr=a.tickRotCorr||{x:0,y:0};f=0===n?-a.labelMetrics().h:2===n?a.tickRotCorr.y:0;h=Math.abs(p)+h;p&&(h=h-f+q*(k?m(z.y,a.tickRotCorr.y+8*q):z.x));a.axisTitleMargin=m(y,h);a.getMaxLabelDimensions&&(a.maxLabelDimensions=a.getMaxLabelDimensions(e,d));k=this.tickSize("tick");g[n]=Math.max(g[n],a.axisTitleMargin+B+q*a.offset,h,d&&d.length&&k?k[0]+
q*a.offset:0);l=l.offset?0:2*Math.floor(a.axisLine.strokeWidth()/2);b[c]=Math.max(b[c],l);C(this,"afterGetOffset")},getLinePath:function(a){var b=this.chart,f=this.opposite,l=this.offset,d=this.horiz,e=this.left+(f?this.width:0)+l;l=b.chartHeight-this.bottom-(f?this.height:0)+l;f&&(a*=-1);return b.renderer.crispLine(["M",d?this.left:e,d?l:this.top,"L",d?b.chartWidth-this.right:e,d?l:b.chartHeight-this.bottom],a)},renderLine:function(){this.axisLine||(this.axisLine=this.chart.renderer.path().addClass("highcharts-axis-line").add(this.axisGroup),
this.chart.styledMode||this.axisLine.attr({stroke:this.options.lineColor,"stroke-width":this.options.lineWidth,zIndex:7}))},getTitlePosition:function(){var a=this.horiz,b=this.left,f=this.top,l=this.len,d=this.options.title,e=a?b:f,k=this.opposite,n=this.offset,c=d.x||0,v=d.y||0,B=this.axisTitle,h=this.chart.renderer.fontMetrics(d.style&&d.style.fontSize,B);B=Math.max(B.getBBox(null,0).height-h.h-1,0);l={low:e+(a?0:l),middle:e+l/2,high:e+(a?l:0)}[d.align];b=(a?f+this.height:b)+(a?1:-1)*(k?-1:1)*this.axisTitleMargin+
[-B,B,h.f,-B][this.side];a={x:a?l+c:b+(k?this.width:0)+n+c,y:a?b+v-(k?this.height:0)+n:l+v};C(this,"afterGetTitlePosition",{titlePosition:a});return a},renderMinorTick:function(a){var b=this.chart.hasRendered&&E(this.oldMin),f=this.minorTicks;f[a]||(f[a]=new B(this,a,"minor"));b&&f[a].isNew&&f[a].render(null,!0);f[a].render(null,!1,1)},renderTick:function(a,b){var f=this.isLinked,l=this.ticks,d=this.chart.hasRendered&&E(this.oldMin);if(!f||a>=this.min&&a<=this.max)l[a]||(l[a]=new B(this,a)),d&&l[a].isNew&&
l[a].render(b,!0,-1),l[a].render(b)},render:function(){var b=this,f=b.chart,l=b.options,d=b.isLog,e=b.isLinked,k=b.tickPositions,n=b.axisTitle,v=b.ticks,h=b.minorTicks,r=b.alternateBands,m=l.stackLabels,z=l.alternateGridColor,p=b.tickmarkOffset,g=b.axisLine,x=b.showAxis,w=a(f.renderer.globalAnimation),y,F;b.labelEdge.length=0;b.overlap=!1;[v,h,r].forEach(function(a){t(a,function(a){a.isActive=!1})});if(b.hasData()||e)b.minorTickInterval&&!b.categories&&b.getMinorTickPositions().forEach(function(a){b.renderMinorTick(a)}),
k.length&&(k.forEach(function(a,f){b.renderTick(a,f)}),p&&(0===b.min||b.single)&&(v[-1]||(v[-1]=new B(b,-1,null,!0)),v[-1].render(-1))),z&&k.forEach(function(a,l){F=void 0!==k[l+1]?k[l+1]+p:b.max-p;0===l%2&&a<b.max&&F<=b.max+(f.polar?-p:p)&&(r[a]||(r[a]=new c.PlotLineOrBand(b)),y=a+p,r[a].options={from:d?b.lin2log(y):y,to:d?b.lin2log(F):F,color:z},r[a].render(),r[a].isActive=!0)}),b._addedPlotLB||((l.plotLines||[]).concat(l.plotBands||[]).forEach(function(a){b.addPlotBandOrLine(a)}),b._addedPlotLB=
!0);[v,h,r].forEach(function(a){var b,l=[],d=w.duration;t(a,function(a,b){a.isActive||(a.render(b,!1,0),a.isActive=!1,l.push(b))});q(function(){for(b=l.length;b--;)a[l[b]]&&!a[l[b]].isActive&&(a[l[b]].destroy(),delete a[l[b]])},a!==r&&f.hasRendered&&d?d:0)});g&&(g[g.isPlaced?"animate":"attr"]({d:this.getLinePath(g.strokeWidth())}),g.isPlaced=!0,g[x?"show":"hide"](x));n&&x&&(l=b.getTitlePosition(),E(l.y)?(n[n.isNew?"attr":"animate"](l),n.isNew=!1):(n.attr("y",-9999),n.isNew=!0));m&&m.enabled&&b.renderStackTotals();
b.isDirty=!1;C(this,"afterRender")},redraw:function(){this.visible&&(this.render(),this.plotLinesAndBands.forEach(function(a){a.render()}));this.series.forEach(function(a){a.isDirty=!0})},keepProps:"extKey hcEvents names series userMax userMin".split(" "),destroy:function(a){var b=this,f=b.stacks,d=b.plotLinesAndBands,e;C(this,"destroy",{keepEvents:a});a||l(b);t(f,function(a,b){y(a);f[b]=null});[b.ticks,b.minorTicks,b.alternateBands].forEach(function(a){y(a)});if(d)for(a=d.length;a--;)d[a].destroy();
"stackTotalGroup axisLine axisTitle axisGroup gridGroup labelGroup cross scrollbar".split(" ").forEach(function(a){b[a]&&(b[a]=b[a].destroy())});for(e in b.plotLinesAndBandsGroups)b.plotLinesAndBandsGroups[e]=b.plotLinesAndBandsGroups[e].destroy();t(b,function(a,f){-1===b.keepProps.indexOf(f)&&delete b[f]})},drawCrosshair:function(a,f){var l,d=this.crosshair,e=m(d.snap,!0),k,n=this.cross;C(this,"drawCrosshair",{e:a,point:f});a||(a=this.cross&&this.cross.e);if(this.crosshair&&!1!==(H(f)||!e)){e?H(f)&&
(k=m("colorAxis"!==this.coll?f.crosshairPos:null,this.isXAxis?f.plotX:this.len-f.plotY)):k=a&&(this.horiz?a.chartX-this.pos:this.len-a.chartY+this.pos);H(k)&&(l=this.getPlotLinePath({value:f&&(this.isXAxis?f.x:m(f.stackY,f.y)),translatedValue:k})||null);if(!H(l)){this.hideCrosshair();return}e=this.categories&&!this.isRadial;n||(this.cross=n=this.chart.renderer.path().addClass("highcharts-crosshair highcharts-crosshair-"+(e?"category ":"thin ")+d.className).attr({zIndex:m(d.zIndex,2)}).add(),this.chart.styledMode||
(n.attr({stroke:d.color||(e?b("#ccd6eb").setOpacity(.25).get():"#cccccc"),"stroke-width":m(d.width,1)}).css({"pointer-events":"none"}),d.dashStyle&&n.attr({dashstyle:d.dashStyle})));n.show().attr({d:l});e&&!d.width&&n.attr({"stroke-width":this.transA});this.cross.e=a}else this.hideCrosshair();C(this,"afterDrawCrosshair",{e:a,point:f})},hideCrosshair:function(){this.cross&&this.cross.hide();C(this,"afterHideCrosshair")}});return c.Axis=g});K(D,"parts/DateTimeAxis.js",[D["parts/Globals.js"]],function(c){var g=
c.Axis,I=c.getMagnitude,G=c.normalizeTickInterval,H=c.timeUnits;g.prototype.getTimeTicks=function(){return this.chart.time.getTimeTicks.apply(this.chart.time,arguments)};g.prototype.normalizeTimeTickInterval=function(c,g){var x=g||[["millisecond",[1,2,5,10,20,25,50,100,200,500]],["second",[1,2,5,10,15,30]],["minute",[1,2,5,10,15,30]],["hour",[1,2,3,4,6,8,12]],["day",[1,2]],["week",[1,2]],["month",[1,2,3,4,6]],["year",null]];g=x[x.length-1];var w=H[g[0]],y=g[1],t;for(t=0;t<x.length&&!(g=x[t],w=H[g[0]],
y=g[1],x[t+1]&&c<=(w*y[y.length-1]+H[x[t+1][0]])/2);t++);w===H.year&&c<5*w&&(y=[1,2,5]);c=G(c/w,y,"year"===g[0]?Math.max(I(c/w),1):1);return{unitRange:w,count:c,unitName:g[0]}}});K(D,"parts/LogarithmicAxis.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.pick;g=c.Axis;var G=c.getMagnitude,H=c.normalizeTickInterval;g.prototype.getLogTickPositions=function(c,g,x,E){var w=this.options,t=this.len,m=[];E||(this._minorAutoInterval=null);if(.5<=c)c=Math.round(c),m=this.getLinearTickPositions(c,
g,x);else if(.08<=c){t=Math.floor(g);var p,q;for(w=.3<c?[1,2,4]:.15<c?[1,2,4,6,8]:[1,2,3,4,5,6,7,8,9];t<x+1&&!q;t++){var h=w.length;for(p=0;p<h&&!q;p++){var a=this.log2lin(this.lin2log(t)*w[p]);a>g&&(!E||b<=x)&&void 0!==b&&m.push(b);b>x&&(q=!0);var b=a}}}else g=this.lin2log(g),x=this.lin2log(x),c=E?this.getMinorTickInterval():w.tickInterval,c=I("auto"===c?null:c,this._minorAutoInterval,w.tickPixelInterval/(E?5:1)*(x-g)/((E?t/this.tickPositions.length:t)||1)),c=H(c,null,G(c)),m=this.getLinearTickPositions(c,
g,x).map(this.log2lin),E||(this._minorAutoInterval=c/5);E||(this.tickInterval=c);return m};g.prototype.log2lin=function(c){return Math.log(c)/Math.LN10};g.prototype.lin2log=function(c){return Math.pow(10,c)}});K(D,"parts/PlotLineOrBand.js",[D["parts/Globals.js"],D["parts/Axis.js"],D["parts/Utilities.js"]],function(c,g,I){var G=I.arrayMax,H=I.arrayMin,y=I.defined,w=I.destroyObjectProperties,x=I.erase,E=I.extend,F=I.objectEach,t=I.pick,m=c.merge;c.PlotLineOrBand=function(c,m){this.axis=c;m&&(this.options=
m,this.id=m.id)};c.PlotLineOrBand.prototype={render:function(){c.fireEvent(this,"render");var p=this,g=p.axis,h=g.horiz,a=p.options,b=a.label,d=p.label,e=a.to,k=a.from,C=a.value,z=y(k)&&y(e),r=y(C),n=p.svgElem,f=!n,l=[],v=a.color,B=t(a.zIndex,0),A=a.events;l={"class":"highcharts-plot-"+(z?"band ":"line ")+(a.className||"")};var u={},J=g.chart.renderer,L=z?"bands":"lines";g.isLog&&(k=g.log2lin(k),e=g.log2lin(e),C=g.log2lin(C));g.chart.styledMode||(r?(l.stroke=v||"#999999",l["stroke-width"]=t(a.width,
1),a.dashStyle&&(l.dashstyle=a.dashStyle)):z&&(l.fill=v||"#e6ebf5",a.borderWidth&&(l.stroke=a.borderColor,l["stroke-width"]=a.borderWidth)));u.zIndex=B;L+="-"+B;(v=g.plotLinesAndBandsGroups[L])||(g.plotLinesAndBandsGroups[L]=v=J.g("plot-"+L).attr(u).add());f&&(p.svgElem=n=J.path().attr(l).add(v));if(r)l=g.getPlotLinePath({value:C,lineWidth:n.strokeWidth(),acrossPanes:a.acrossPanes});else if(z)l=g.getPlotBandPath(k,e,a);else return;(f||!n.d)&&l&&l.length?(n.attr({d:l}),A&&F(A,function(a,b){n.on(b,
function(a){A[b].apply(p,[a])})})):n&&(l?(n.show(!0),n.animate({d:l})):n.d&&(n.hide(),d&&(p.label=d=d.destroy())));b&&(y(b.text)||y(b.formatter))&&l&&l.length&&0<g.width&&0<g.height&&!l.isFlat?(b=m({align:h&&z&&"center",x:h?!z&&4:10,verticalAlign:!h&&z&&"middle",y:h?z?16:10:z?6:-4,rotation:h&&!z&&90},b),this.renderLabel(b,l,z,B)):d&&d.hide();return p},renderLabel:function(c,m,h,a){var b=this.label,d=this.axis.chart.renderer;b||(b={align:c.textAlign||c.align,rotation:c.rotation,"class":"highcharts-plot-"+
(h?"band":"line")+"-label "+(c.className||"")},b.zIndex=a,a=this.getLabelText(c),this.label=b=d.text(a,0,0,c.useHTML).attr(b).add(),this.axis.chart.styledMode||b.css(c.style));d=m.xBounds||[m[1],m[4],h?m[6]:m[1]];m=m.yBounds||[m[2],m[5],h?m[7]:m[2]];h=H(d);a=H(m);b.align(c,!1,{x:h,y:a,width:G(d)-h,height:G(m)-a});b.show(!0)},getLabelText:function(c){return y(c.formatter)?c.formatter.call(this):c.text},destroy:function(){x(this.axis.plotLinesAndBands,this);delete this.axis;w(this)}};E(g.prototype,
{getPlotBandPath:function(c,m){var h=this.getPlotLinePath({value:m,force:!0,acrossPanes:this.options.acrossPanes}),a=this.getPlotLinePath({value:c,force:!0,acrossPanes:this.options.acrossPanes}),b=[],d=this.horiz,e=1;c=c<this.min&&m<this.min||c>this.max&&m>this.max;if(a&&h){if(c){var k=a.toString()===h.toString();e=0}for(c=0;c<a.length;c+=6)d&&h[c+1]===a[c+1]?(h[c+1]+=e,h[c+4]+=e):d||h[c+2]!==a[c+2]||(h[c+2]+=e,h[c+5]+=e),b.push("M",a[c+1],a[c+2],"L",a[c+4],a[c+5],h[c+4],h[c+5],h[c+1],h[c+2],"z"),
b.isFlat=k}return b},addPlotBand:function(c){return this.addPlotBandOrLine(c,"plotBands")},addPlotLine:function(c){return this.addPlotBandOrLine(c,"plotLines")},addPlotBandOrLine:function(m,g){var h=(new c.PlotLineOrBand(this,m)).render(),a=this.userOptions;if(h){if(g){var b=a[g]||[];b.push(m);a[g]=b}this.plotLinesAndBands.push(h)}return h},removePlotBandOrLine:function(c){for(var m=this.plotLinesAndBands,h=this.options,a=this.userOptions,b=m.length;b--;)m[b].id===c&&m[b].destroy();[h.plotLines||
[],a.plotLines||[],h.plotBands||[],a.plotBands||[]].forEach(function(a){for(b=a.length;b--;)a[b].id===c&&x(a,a[b])})},removePlotBand:function(c){this.removePlotBandOrLine(c)},removePlotLine:function(c){this.removePlotBandOrLine(c)}})});K(D,"parts/Tooltip.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.discardElement,H=g.extend,y=g.isNumber,w=g.isString,x=g.pick,E=g.splat,F=g.syncTimeout;"";var t=c.doc,m=c.format,p=c.merge,q=c.timeUnits;c.Tooltip=function(){this.init.apply(this,
arguments)};c.Tooltip.prototype={init:function(c,a){this.chart=c;this.options=a;this.crosshairs=[];this.now={x:0,y:0};this.isHidden=!0;this.split=a.split&&!c.inverted;this.shared=a.shared||this.split;this.outside=x(a.outside,!(!c.scrollablePixelsX&&!c.scrollablePixelsY))},cleanSplit:function(c){this.chart.series.forEach(function(a){var b=a&&a.tt;b&&(!b.isActive||c?a.tt=b.destroy():b.isActive=!1)})},applyFilter:function(){var c=this.chart;c.renderer.definition({tagName:"filter",id:"drop-shadow-"+c.index,
opacity:.5,children:[{tagName:"feGaussianBlur","in":"SourceAlpha",stdDeviation:1},{tagName:"feOffset",dx:1,dy:1},{tagName:"feComponentTransfer",children:[{tagName:"feFuncA",type:"linear",slope:.3}]},{tagName:"feMerge",children:[{tagName:"feMergeNode"},{tagName:"feMergeNode","in":"SourceGraphic"}]}]});c.renderer.definition({tagName:"style",textContent:".highcharts-tooltip-"+c.index+"{filter:url(#drop-shadow-"+c.index+")}"})},getLabel:function(){var h=this,a=this.chart.renderer,b=this.chart.styledMode,
d=this.options,e="tooltip"+(I(d.className)?" "+d.className:""),k;if(!this.label){this.outside&&(this.container=k=c.doc.createElement("div"),k.className="highcharts-tooltip-container",c.css(k,{position:"absolute",top:"1px",pointerEvents:d.style&&d.style.pointerEvents,zIndex:3}),c.doc.body.appendChild(k),this.renderer=a=new c.Renderer(k,0,0,{},void 0,void 0,a.styledMode));this.split?this.label=a.g(e):(this.label=a.label("",0,0,d.shape||"callout",null,null,d.useHTML,null,e).attr({padding:d.padding,r:d.borderRadius}),
b||this.label.attr({fill:d.backgroundColor,"stroke-width":d.borderWidth}).css(d.style).shadow(d.shadow));b&&(this.applyFilter(),this.label.addClass("highcharts-tooltip-"+this.chart.index));if(h.outside&&!h.split){var m={x:this.label.xSetter,y:this.label.ySetter};this.label.xSetter=function(a,b){m[b].call(this.label,h.distance);k.style.left=a+"px"};this.label.ySetter=function(a,b){m[b].call(this.label,h.distance);k.style.top=a+"px"}}this.label.attr({zIndex:8}).add()}return this.label},update:function(c){this.destroy();
p(!0,this.chart.options.tooltip.userOptions,c);this.init(this.chart,p(!0,this.options,c))},destroy:function(){this.label&&(this.label=this.label.destroy());this.split&&this.tt&&(this.cleanSplit(this.chart,!0),this.tt=this.tt.destroy());this.renderer&&(this.renderer=this.renderer.destroy(),G(this.container));c.clearTimeout(this.hideTimer);c.clearTimeout(this.tooltipTimeout)},move:function(h,a,b,d){var e=this,k=e.now,m=!1!==e.options.animation&&!e.isHidden&&(1<Math.abs(h-k.x)||1<Math.abs(a-k.y)),z=
e.followPointer||1<e.len;H(k,{x:m?(2*k.x+h)/3:h,y:m?(k.y+a)/2:a,anchorX:z?void 0:m?(2*k.anchorX+b)/3:b,anchorY:z?void 0:m?(k.anchorY+d)/2:d});e.getLabel().attr(k);m&&(c.clearTimeout(this.tooltipTimeout),this.tooltipTimeout=setTimeout(function(){e&&e.move(h,a,b,d)},32))},hide:function(h){var a=this;c.clearTimeout(this.hideTimer);h=x(h,this.options.hideDelay,500);this.isHidden||(this.hideTimer=F(function(){a.getLabel()[h?"fadeOut":"hide"]();a.isHidden=!0},h))},getAnchor:function(c,a){var b=this.chart,
d=b.pointer,e=b.inverted,k=b.plotTop,h=b.plotLeft,m=0,r=0,n,f;c=E(c);this.followPointer&&a?(void 0===a.chartX&&(a=d.normalize(a)),c=[a.chartX-b.plotLeft,a.chartY-k]):c[0].tooltipPos?c=c[0].tooltipPos:(c.forEach(function(a){n=a.series.yAxis;f=a.series.xAxis;m+=a.plotX+(!e&&f?f.left-h:0);r+=(a.plotLow?(a.plotLow+a.plotHigh)/2:a.plotY)+(!e&&n?n.top-k:0)}),m/=c.length,r/=c.length,c=[e?b.plotWidth-r:m,this.shared&&!e&&1<c.length&&a?a.chartY-k:e?b.plotHeight-m:r]);return c.map(Math.round)},getPosition:function(c,
a,b){var d=this.chart,e=this.distance,k={},h=d.inverted&&b.h||0,m,r=this.outside,n=r?t.documentElement.clientWidth-2*e:d.chartWidth,f=r?Math.max(t.body.scrollHeight,t.documentElement.scrollHeight,t.body.offsetHeight,t.documentElement.offsetHeight,t.documentElement.clientHeight):d.chartHeight,l=d.pointer.getChartPosition(),v=d.containerScaling,B=function(a){return v?a*v.scaleX:a},A=function(a){return v?a*v.scaleY:a},u=function(k){var v="x"===k;return[k,v?n:f,v?c:a].concat(r?[v?B(c):A(a),v?l.left-e+
B(b.plotX+d.plotLeft):l.top-e+A(b.plotY+d.plotTop),0,v?n:f]:[v?c:a,v?b.plotX+d.plotLeft:b.plotY+d.plotTop,v?d.plotLeft:d.plotTop,v?d.plotLeft+d.plotWidth:d.plotTop+d.plotHeight])},g=u("y"),p=u("x"),q=!this.followPointer&&x(b.ttBelow,!d.inverted===!!b.negative),w=function(a,b,f,l,d,c,n){var v="y"===a?A(e):B(e),u=(f-l)/2,r=l<d-e,m=d+e+l<b,C=d-v-f+u;d=d+v-u;if(q&&m)k[a]=d;else if(!q&&r)k[a]=C;else if(r)k[a]=Math.min(n-l,0>C-h?C:C-h);else if(m)k[a]=Math.max(c,d+h+f>b?d:d+h);else return!1},E=function(a,
b,f,l,d){var c;d<e||d>b-e?c=!1:k[a]=d<f/2?1:d>b-l/2?b-l-2:d-f/2;return c},M=function(a){var b=g;g=p;p=b;m=a},N=function(){!1!==w.apply(0,g)?!1!==E.apply(0,p)||m||(M(!0),N()):m?k.x=k.y=0:(M(!0),N())};(d.inverted||1<this.len)&&M();N();return k},defaultFormatter:function(c){var a=this.points||E(this);var b=[c.tooltipFooterHeaderFormatter(a[0])];b=b.concat(c.bodyFormatter(a));b.push(c.tooltipFooterHeaderFormatter(a[0],!0));return b},refresh:function(h,a){var b=this.chart,d=this.options,e=h,k={},m=[],
z=d.formatter||this.defaultFormatter;k=this.shared;var r=b.styledMode;if(d.enabled){c.clearTimeout(this.hideTimer);this.followPointer=E(e)[0].series.tooltipOptions.followPointer;var n=this.getAnchor(e,a);a=n[0];var f=n[1];!k||e.series&&e.series.noSharedTooltip?k=e.getLabelConfig():(b.pointer.applyInactiveState(e),e.forEach(function(a){a.setState("hover");m.push(a.getLabelConfig())}),k={x:e[0].category,y:e[0].y},k.points=m,e=e[0]);this.len=m.length;b=z.call(k,this);z=e.series;this.distance=x(z.tooltipOptions.distance,
16);!1===b?this.hide():(this.split?this.renderSplit(b,E(h)):(h=this.getLabel(),d.style.width&&!r||h.css({width:this.chart.spacingBox.width}),h.attr({text:b&&b.join?b.join(""):b}),h.removeClass(/highcharts-color-[\d]+/g).addClass("highcharts-color-"+x(e.colorIndex,z.colorIndex)),r||h.attr({stroke:d.borderColor||e.color||z.color||"#666666"}),this.updatePosition({plotX:a,plotY:f,negative:e.negative,ttBelow:e.ttBelow,h:n[2]||0})),this.isHidden&&this.label&&this.label.attr({opacity:1}).show(),this.isHidden=
!1);c.fireEvent(this,"refresh")}},renderSplit:function(h,a){var b=this,d=[],e=this.chart,k=e.renderer,m=!0,z=this.options,r=0,n,f=this.getLabel(),l=e.plotTop;w(h)&&(h=[!1,h]);h.slice(0,a.length+1).forEach(function(c,v){if(!1!==c&&""!==c){v=a[v-1]||{isHeader:!0,plotX:a[0].plotX,plotY:e.plotHeight};var B=v.series||b,u=B.tt,h=v.series||{},A="highcharts-color-"+x(v.colorIndex,h.colorIndex,"none");u||(u={padding:z.padding,r:z.borderRadius},e.styledMode||(u.fill=z.backgroundColor,u["stroke-width"]=z.borderWidth),
B.tt=u=k.label(null,null,null,(v.isHeader?z.headerShape:z.shape)||"callout",null,null,z.useHTML).addClass(v.isHeader?"highcharts-tooltip-header ":"highcharts-tooltip-box "+A).attr(u).add(f));u.isActive=!0;u.attr({text:c});e.styledMode||u.css(z.style).shadow(z.shadow).attr({stroke:z.borderColor||v.color||h.color||"#333333"});c=u.getBBox();A=c.width+u.strokeWidth();v.isHeader?(r=c.height,e.xAxis[0].opposite&&(n=!0,l-=r),c=Math.max(0,Math.min(v.plotX+e.plotLeft-A/2,e.chartWidth+(e.scrollablePixelsX?
e.scrollablePixelsX-e.marginRight:0)-A))):c=v.plotX+e.plotLeft-x(z.distance,16)-A;0>c&&(m=!1);v.isHeader?h=n?-r:e.plotHeight+r:(h=h.yAxis,h=h.pos-l+Math.max(0,Math.min(v.plotY||0,h.len)));d.push({target:h,rank:v.isHeader?1:0,size:B.tt.getBBox().height+1,point:v,x:c,tt:u})}});this.cleanSplit();z.positioner&&d.forEach(function(a){var f=z.positioner.call(b,a.tt.getBBox().width,a.size,a.point);a.x=f.x;a.align=0;a.target=f.y;a.rank=x(f.rank,a.rank)});c.distribute(d,e.plotHeight+r);d.forEach(function(a){var f=
a.point,d=f.series,c=d&&d.yAxis;a.tt.attr({visibility:void 0===a.pos?"hidden":"inherit",x:m||f.isHeader||z.positioner?a.x:f.plotX+e.plotLeft+b.distance,y:a.pos+l,anchorX:f.isHeader?f.plotX+e.plotLeft:f.plotX+d.xAxis.pos,anchorY:f.isHeader?e.plotTop+e.plotHeight/2:c.pos+Math.max(0,Math.min(f.plotY,c.len))})});var v=b.container;h=b.renderer;if(b.outside&&v&&h){var B=e.pointer.getChartPosition();v.style.left=B.left+"px";v.style.top=B.top+"px";v=f.getBBox();h.setSize(v.width+v.x,v.height+v.y,!1)}},updatePosition:function(h){var a=
this.chart,b=a.pointer,d=this.getLabel(),e=h.plotX+a.plotLeft,k=h.plotY+a.plotTop;b=b.getChartPosition();h=(this.options.positioner||this.getPosition).call(this,d.width,d.height,h);if(this.outside){var m=(this.options.borderWidth||0)+2*this.distance;this.renderer.setSize(d.width+m,d.height+m,!1);if(a=a.containerScaling)c.css(this.container,{transform:"scale("+a.scaleX+", "+a.scaleY+")"}),e*=a.scaleX,k*=a.scaleY;e+=b.left-h.x;k+=b.top-h.y}this.move(Math.round(h.x),Math.round(h.y||0),e,k)},getDateFormat:function(c,
a,b,d){var e=this.chart.time,k=e.dateFormat("%m-%d %H:%M:%S.%L",a),h={millisecond:15,second:12,minute:9,hour:6,day:3},m="millisecond";for(r in q){if(c===q.week&&+e.dateFormat("%w",a)===b&&"00:00:00.000"===k.substr(6)){var r="week";break}if(q[r]>c){r=m;break}if(h[r]&&k.substr(h[r])!=="01-01 00:00:00.000".substr(h[r]))break;"week"!==r&&(m=r)}if(r)var n=e.resolveDTLFormat(d[r]).main;return n},getXDateFormat:function(c,a,b){a=a.dateTimeLabelFormats;var d=b&&b.closestPointRange;return(d?this.getDateFormat(d,
c.x,b.options.startOfWeek,a):a.day)||a.year},tooltipFooterHeaderFormatter:function(h,a){var b=a?"footer":"header",d=h.series,e=d.tooltipOptions,k=e.xDateFormat,C=d.xAxis,z=C&&"datetime"===C.options.type&&y(h.key),r=e[b+"Format"];a={isFooter:a,labelConfig:h};c.fireEvent(this,"headerFormatter",a,function(a){z&&!k&&(k=this.getXDateFormat(h,e,C));z&&k&&(h.point&&h.point.tooltipDateKeys||["key"]).forEach(function(a){r=r.replace("{point."+a+"}","{point."+a+":"+k+"}")});d.chart.styledMode&&(r=this.styledModeFormat(r));
a.text=m(r,{point:h,series:d},this.chart.time)});return a.text},bodyFormatter:function(c){return c.map(function(a){var b=a.series.tooltipOptions;return(b[(a.point.formatPrefix||"point")+"Formatter"]||a.point.tooltipFormatter).call(a.point,b[(a.point.formatPrefix||"point")+"Format"]||"")})},styledModeFormat:function(c){return c.replace('style="font-size: 10px"','class="highcharts-header"').replace(/style="color:{(point|series)\.color}"/g,'class="highcharts-color-{$1.colorIndex}"')}}});K(D,"parts/Pointer.js",
[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.attr,G=g.defined,H=g.extend,y=g.isNumber,w=g.isObject,x=g.objectEach,E=g.pick,F=g.splat,t=c.addEvent,m=c.charts,p=c.color,q=c.css,h=c.find,a=c.fireEvent,b=c.offset,d=c.Tooltip;c.Pointer=function(a,b){this.init(a,b)};c.Pointer.prototype={init:function(a,b){this.options=b;this.chart=a;this.runChartClick=b.chart.events&&!!b.chart.events.click;this.pinchDown=[];this.lastValidTouch={};d&&(a.tooltip=new d(a,b.tooltip),this.followTouchMove=
E(b.tooltip.followTouchMove,!0));this.setDOMEvents()},zoomOption:function(a){var b=this.chart,d=b.options.chart,e=d.zoomType||"";b=b.inverted;/touch/.test(a.type)&&(e=E(d.pinchType,e));this.zoomX=a=/x/.test(e);this.zoomY=e=/y/.test(e);this.zoomHor=a&&!b||e&&b;this.zoomVert=e&&!b||a&&b;this.hasZoom=a||e},getChartPosition:function(){return this.chartPosition||(this.chartPosition=b(this.chart.container))},normalize:function(a,b){var d=a.touches?a.touches.length?a.touches.item(0):a.changedTouches[0]:
a;b||(b=this.getChartPosition());var e=d.pageX-b.left;b=d.pageY-b.top;if(d=this.chart.containerScaling)e/=d.scaleX,b/=d.scaleY;return H(a,{chartX:Math.round(e),chartY:Math.round(b)})},getCoordinates:function(a){var b={xAxis:[],yAxis:[]};this.chart.axes.forEach(function(d){b[d.isXAxis?"xAxis":"yAxis"].push({axis:d,value:d.toValue(a[d.horiz?"chartX":"chartY"])})});return b},findNearestKDPoint:function(a,b,d){var e;a.forEach(function(a){var c=!(a.noSharedTooltip&&b)&&0>a.options.findNearestPointBy.indexOf("y");
a=a.searchPoint(d,c);if((c=w(a,!0))&&!(c=!w(e,!0))){c=e.distX-a.distX;var f=e.dist-a.dist,l=(a.series.group&&a.series.group.zIndex)-(e.series.group&&e.series.group.zIndex);c=0<(0!==c&&b?c:0!==f?f:0!==l?l:e.series.index>a.series.index?-1:1)}c&&(e=a)});return e},getPointFromEvent:function(a){a=a.target;for(var b;a&&!b;)b=a.point,a=a.parentNode;return b},getChartCoordinatesFromPoint:function(a,b){var d=a.series,e=d.xAxis;d=d.yAxis;var c=E(a.clientX,a.plotX),k=a.shapeArgs;if(e&&d)return b?{chartX:e.len+
e.pos-c,chartY:d.len+d.pos-a.plotY}:{chartX:c+e.pos,chartY:a.plotY+d.pos};if(k&&k.x&&k.y)return{chartX:k.x,chartY:k.y}},getHoverData:function(a,b,d,c,m,n){var f,l=[];c=!(!c||!a);var e=b&&!b.stickyTracking?[b]:d.filter(function(a){return a.visible&&!(!m&&a.directTouch)&&E(a.options.enableMouseTracking,!0)&&a.stickyTracking});b=(f=c||!n?a:this.findNearestKDPoint(e,m,n))&&f.series;f&&(m&&!b.noSharedTooltip?(e=d.filter(function(a){return a.visible&&!(!m&&a.directTouch)&&E(a.options.enableMouseTracking,
!0)&&!a.noSharedTooltip}),e.forEach(function(a){var b=h(a.points,function(a){return a.x===f.x&&!a.isNull});w(b)&&(a.chart.isBoosting&&(b=a.getPoint(b)),l.push(b))})):l.push(f));return{hoverPoint:f,hoverSeries:b,hoverPoints:l}},runPointActions:function(a,b){var d=this.chart,e=d.tooltip&&d.tooltip.options.enabled?d.tooltip:void 0,k=e?e.shared:!1,n=b||d.hoverPoint,f=n&&n.series||d.hoverSeries;f=this.getHoverData(n,f,d.series,(!a||"touchmove"!==a.type)&&(!!b||f&&f.directTouch&&this.isDirectTouch),k,a);
n=f.hoverPoint;var l=f.hoverPoints;b=(f=f.hoverSeries)&&f.tooltipOptions.followPointer;k=k&&f&&!f.noSharedTooltip;if(n&&(n!==d.hoverPoint||e&&e.isHidden)){(d.hoverPoints||[]).forEach(function(a){-1===l.indexOf(a)&&a.setState()});if(d.hoverSeries!==f)f.onMouseOver();this.applyInactiveState(l);(l||[]).forEach(function(a){a.setState("hover")});d.hoverPoint&&d.hoverPoint.firePointEvent("mouseOut");if(!n.series)return;n.firePointEvent("mouseOver");d.hoverPoints=l;d.hoverPoint=n;e&&e.refresh(k?l:n,a)}else b&&
e&&!e.isHidden&&(n=e.getAnchor([{}],a),e.updatePosition({plotX:n[0],plotY:n[1]}));this.unDocMouseMove||(this.unDocMouseMove=t(d.container.ownerDocument,"mousemove",function(a){var b=m[c.hoverChartIndex];if(b)b.pointer.onDocumentMouseMove(a)}));d.axes.forEach(function(b){var f=E(b.crosshair.snap,!0),d=f?c.find(l,function(a){return a.series[b.coll]===b}):void 0;d||!f?b.drawCrosshair(a,d):b.hideCrosshair()})},applyInactiveState:function(a){var b=[],d;(a||[]).forEach(function(a){d=a.series;b.push(d);
d.linkedParent&&b.push(d.linkedParent);d.linkedSeries&&(b=b.concat(d.linkedSeries));d.navigatorSeries&&b.push(d.navigatorSeries)});this.chart.series.forEach(function(a){-1===b.indexOf(a)?a.setState("inactive",!0):a.options.inactiveOtherPoints&&a.setAllPointsToState("inactive")})},reset:function(a,b){var d=this.chart,c=d.hoverSeries,e=d.hoverPoint,k=d.hoverPoints,f=d.tooltip,l=f&&f.shared?k:e;a&&l&&F(l).forEach(function(b){b.series.isCartesian&&void 0===b.plotX&&(a=!1)});if(a)f&&l&&F(l).length&&(f.refresh(l),
f.shared&&k?k.forEach(function(a){a.setState(a.state,!0);a.series.isCartesian&&(a.series.xAxis.crosshair&&a.series.xAxis.drawCrosshair(null,a),a.series.yAxis.crosshair&&a.series.yAxis.drawCrosshair(null,a))}):e&&(e.setState(e.state,!0),d.axes.forEach(function(a){a.crosshair&&a.drawCrosshair(null,e)})));else{if(e)e.onMouseOut();k&&k.forEach(function(a){a.setState()});if(c)c.onMouseOut();f&&f.hide(b);this.unDocMouseMove&&(this.unDocMouseMove=this.unDocMouseMove());d.axes.forEach(function(a){a.hideCrosshair()});
this.hoverX=d.hoverPoints=d.hoverPoint=null}},scaleGroups:function(a,b){var d=this.chart,e;d.series.forEach(function(c){e=a||c.getPlotBox();c.xAxis&&c.xAxis.zoomEnabled&&c.group&&(c.group.attr(e),c.markerGroup&&(c.markerGroup.attr(e),c.markerGroup.clip(b?d.clipRect:null)),c.dataLabelsGroup&&c.dataLabelsGroup.attr(e))});d.clipRect.attr(b||d.clipBox)},dragStart:function(a){var b=this.chart;b.mouseIsDown=a.type;b.cancelClick=!1;b.mouseDownX=this.mouseDownX=a.chartX;b.mouseDownY=this.mouseDownY=a.chartY},
drag:function(a){var b=this.chart,d=b.options.chart,c=a.chartX,e=a.chartY,n=this.zoomHor,f=this.zoomVert,l=b.plotLeft,v=b.plotTop,B=b.plotWidth,h=b.plotHeight,u=this.selectionMarker,m=this.mouseDownX,g=this.mouseDownY,q=d.panKey&&a[d.panKey+"Key"];if(!u||!u.touch)if(c<l?c=l:c>l+B&&(c=l+B),e<v?e=v:e>v+h&&(e=v+h),this.hasDragged=Math.sqrt(Math.pow(m-c,2)+Math.pow(g-e,2)),10<this.hasDragged){var t=b.isInsidePlot(m-l,g-v);b.hasCartesianSeries&&(this.zoomX||this.zoomY)&&t&&!q&&!u&&(this.selectionMarker=
u=b.renderer.rect(l,v,n?1:B,f?1:h,0).attr({"class":"highcharts-selection-marker",zIndex:7}).add(),b.styledMode||u.attr({fill:d.selectionMarkerFill||p("#335cad").setOpacity(.25).get()}));u&&n&&(c-=m,u.attr({width:Math.abs(c),x:(0<c?0:c)+m}));u&&f&&(c=e-g,u.attr({height:Math.abs(c),y:(0<c?0:c)+g}));t&&!u&&d.panning&&b.pan(a,d.panning)}},drop:function(b){var d=this,c=this.chart,e=this.hasPinched;if(this.selectionMarker){var h={originalEvent:b,xAxis:[],yAxis:[]},n=this.selectionMarker,f=n.attr?n.attr("x"):
n.x,l=n.attr?n.attr("y"):n.y,v=n.attr?n.attr("width"):n.width,B=n.attr?n.attr("height"):n.height,m;if(this.hasDragged||e)c.axes.forEach(function(a){if(a.zoomEnabled&&G(a.min)&&(e||d[{xAxis:"zoomX",yAxis:"zoomY"}[a.coll]])){var c=a.horiz,k="touchend"===b.type?a.minPixelPadding:0,n=a.toValue((c?f:l)+k);c=a.toValue((c?f+v:l+B)-k);h[a.coll].push({axis:a,min:Math.min(n,c),max:Math.max(n,c)});m=!0}}),m&&a(c,"selection",h,function(a){c.zoom(H(a,e?{animation:!1}:null))});y(c.index)&&(this.selectionMarker=
this.selectionMarker.destroy());e&&this.scaleGroups()}c&&y(c.index)&&(q(c.container,{cursor:c._cursor}),c.cancelClick=10<this.hasDragged,c.mouseIsDown=this.hasDragged=this.hasPinched=!1,this.pinchDown=[])},onContainerMouseDown:function(a){a=this.normalize(a);2!==a.button&&(this.zoomOption(a),a.preventDefault&&a.preventDefault(),this.dragStart(a))},onDocumentMouseUp:function(a){m[c.hoverChartIndex]&&m[c.hoverChartIndex].pointer.drop(a)},onDocumentMouseMove:function(a){var b=this.chart,d=this.chartPosition;
a=this.normalize(a,d);!d||this.inClass(a.target,"highcharts-tracker")||b.isInsidePlot(a.chartX-b.plotLeft,a.chartY-b.plotTop)||this.reset()},onContainerMouseLeave:function(a){var b=m[c.hoverChartIndex];b&&(a.relatedTarget||a.toElement)&&(b.pointer.reset(),b.pointer.chartPosition=void 0)},onContainerMouseMove:function(a){var b=this.chart;G(c.hoverChartIndex)&&m[c.hoverChartIndex]&&m[c.hoverChartIndex].mouseIsDown||(c.hoverChartIndex=b.index);a=this.normalize(a);a.preventDefault||(a.returnValue=!1);
"mousedown"===b.mouseIsDown&&this.drag(a);!this.inClass(a.target,"highcharts-tracker")&&!b.isInsidePlot(a.chartX-b.plotLeft,a.chartY-b.plotTop)||b.openMenu||this.runPointActions(a)},inClass:function(a,b){for(var d;a;){if(d=I(a,"class")){if(-1!==d.indexOf(b))return!0;if(-1!==d.indexOf("highcharts-container"))return!1}a=a.parentNode}},onTrackerMouseOut:function(a){var b=this.chart.hoverSeries;a=a.relatedTarget||a.toElement;this.isDirectTouch=!1;if(!(!b||!a||b.stickyTracking||this.inClass(a,"highcharts-tooltip")||
this.inClass(a,"highcharts-series-"+b.index)&&this.inClass(a,"highcharts-tracker")))b.onMouseOut()},onContainerClick:function(b){var d=this.chart,c=d.hoverPoint,e=d.plotLeft,h=d.plotTop;b=this.normalize(b);d.cancelClick||(c&&this.inClass(b.target,"highcharts-tracker")?(a(c.series,"click",H(b,{point:c})),d.hoverPoint&&c.firePointEvent("click",b)):(H(b,this.getCoordinates(b)),d.isInsidePlot(b.chartX-e,b.chartY-h)&&a(d,"click",b)))},setDOMEvents:function(){var a=this,b=a.chart.container,d=b.ownerDocument;
b.onmousedown=function(b){a.onContainerMouseDown(b)};b.onmousemove=function(b){a.onContainerMouseMove(b)};b.onclick=function(b){a.onContainerClick(b)};this.unbindContainerMouseLeave=t(b,"mouseleave",a.onContainerMouseLeave);c.unbindDocumentMouseUp||(c.unbindDocumentMouseUp=t(d,"mouseup",a.onDocumentMouseUp));c.hasTouch&&(t(b,"touchstart",function(b){a.onContainerTouchStart(b)}),t(b,"touchmove",function(b){a.onContainerTouchMove(b)}),c.unbindDocumentTouchEnd||(c.unbindDocumentTouchEnd=t(d,"touchend",
a.onDocumentTouchEnd)))},destroy:function(){var a=this;a.unDocMouseMove&&a.unDocMouseMove();this.unbindContainerMouseLeave();c.chartCount||(c.unbindDocumentMouseUp&&(c.unbindDocumentMouseUp=c.unbindDocumentMouseUp()),c.unbindDocumentTouchEnd&&(c.unbindDocumentTouchEnd=c.unbindDocumentTouchEnd()));clearInterval(a.tooltipTimeout);x(a,function(b,d){a[d]=null})}}});K(D,"parts/TouchPointer.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.extend,G=g.pick,H=c.charts,y=c.noop;I(c.Pointer.prototype,
{pinchTranslate:function(c,g,E,y,t,m){this.zoomHor&&this.pinchTranslateDirection(!0,c,g,E,y,t,m);this.zoomVert&&this.pinchTranslateDirection(!1,c,g,E,y,t,m)},pinchTranslateDirection:function(c,g,E,y,t,m,p,q){var h=this.chart,a=c?"x":"y",b=c?"X":"Y",d="chart"+b,e=c?"width":"height",k=h["plot"+(c?"Left":"Top")],C,z,r=q||1,n=h.inverted,f=h.bounds[c?"h":"v"],l=1===g.length,v=g[0][d],B=E[0][d],A=!l&&g[1][d],u=!l&&E[1][d];E=function(){!l&&20<Math.abs(v-A)&&(r=q||Math.abs(B-u)/Math.abs(v-A));z=(k-B)/r+v;
C=h["plot"+(c?"Width":"Height")]/r};E();g=z;if(g<f.min){g=f.min;var J=!0}else g+C>f.max&&(g=f.max-C,J=!0);J?(B-=.8*(B-p[a][0]),l||(u-=.8*(u-p[a][1])),E()):p[a]=[B,u];n||(m[a]=z-k,m[e]=C);m=n?1/r:r;t[e]=C;t[a]=g;y[n?c?"scaleY":"scaleX":"scale"+b]=r;y["translate"+b]=m*k+(B-m*v)},pinch:function(c){var g=this,w=g.chart,F=g.pinchDown,t=c.touches,m=t.length,p=g.lastValidTouch,q=g.hasZoom,h=g.selectionMarker,a={},b=1===m&&(g.inClass(c.target,"highcharts-tracker")&&w.runTrackerClick||g.runChartClick),d={};
1<m&&(g.initiated=!0);q&&g.initiated&&!b&&c.preventDefault();[].map.call(t,function(a){return g.normalize(a)});"touchstart"===c.type?([].forEach.call(t,function(a,b){F[b]={chartX:a.chartX,chartY:a.chartY}}),p.x=[F[0].chartX,F[1]&&F[1].chartX],p.y=[F[0].chartY,F[1]&&F[1].chartY],w.axes.forEach(function(a){if(a.zoomEnabled){var b=w.bounds[a.horiz?"h":"v"],d=a.minPixelPadding,c=a.toPixels(Math.min(G(a.options.min,a.dataMin),a.dataMin)),e=a.toPixels(Math.max(G(a.options.max,a.dataMax),a.dataMax)),n=Math.max(c,
e);b.min=Math.min(a.pos,Math.min(c,e)-d);b.max=Math.max(a.pos+a.len,n+d)}}),g.res=!0):g.followTouchMove&&1===m?this.runPointActions(g.normalize(c)):F.length&&(h||(g.selectionMarker=h=I({destroy:y,touch:!0},w.plotBox)),g.pinchTranslate(F,t,a,h,d,p),g.hasPinched=q,g.scaleGroups(a,d),g.res&&(g.res=!1,this.reset(!1,0)))},touch:function(g,x){var w=this.chart,y;if(w.index!==c.hoverChartIndex)this.onContainerMouseLeave({relatedTarget:!0});c.hoverChartIndex=w.index;if(1===g.touches.length)if(g=this.normalize(g),
(y=w.isInsidePlot(g.chartX-w.plotLeft,g.chartY-w.plotTop))&&!w.openMenu){x&&this.runPointActions(g);if("touchmove"===g.type){x=this.pinchDown;var t=x[0]?4<=Math.sqrt(Math.pow(x[0].chartX-g.chartX,2)+Math.pow(x[0].chartY-g.chartY,2)):!1}G(t,!0)&&this.pinch(g)}else x&&this.reset();else 2===g.touches.length&&this.pinch(g)},onContainerTouchStart:function(c){this.zoomOption(c);this.touch(c,!0)},onContainerTouchMove:function(c){this.touch(c)},onDocumentTouchEnd:function(g){H[c.hoverChartIndex]&&H[c.hoverChartIndex].pointer.drop(g)}})});
K(D,"parts/MSPointer.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.extend,G=g.objectEach,H=c.addEvent,y=c.charts,w=c.css,x=c.doc,E=c.noop;g=c.Pointer;var F=c.removeEvent,t=c.win,m=c.wrap;if(!c.hasTouch&&(t.PointerEvent||t.MSPointerEvent)){var p={},q=!!t.PointerEvent,h=function(){var a=[];a.item=function(a){return this[a]};G(p,function(b){a.push({pageX:b.pageX,pageY:b.pageY,target:b.target})});return a},a=function(a,d,e,k){"touch"!==a.pointerType&&a.pointerType!==a.MSPOINTER_TYPE_TOUCH||
!y[c.hoverChartIndex]||(k(a),k=y[c.hoverChartIndex].pointer,k[d]({type:e,target:a.currentTarget,preventDefault:E,touches:h()}))};I(g.prototype,{onContainerPointerDown:function(b){a(b,"onContainerTouchStart","touchstart",function(a){p[a.pointerId]={pageX:a.pageX,pageY:a.pageY,target:a.currentTarget}})},onContainerPointerMove:function(b){a(b,"onContainerTouchMove","touchmove",function(a){p[a.pointerId]={pageX:a.pageX,pageY:a.pageY};p[a.pointerId].target||(p[a.pointerId].target=a.currentTarget)})},onDocumentPointerUp:function(b){a(b,
"onDocumentTouchEnd","touchend",function(a){delete p[a.pointerId]})},batchMSEvents:function(a){a(this.chart.container,q?"pointerdown":"MSPointerDown",this.onContainerPointerDown);a(this.chart.container,q?"pointermove":"MSPointerMove",this.onContainerPointerMove);a(x,q?"pointerup":"MSPointerUp",this.onDocumentPointerUp)}});m(g.prototype,"init",function(a,d,c){a.call(this,d,c);this.hasZoom&&w(d.container,{"-ms-touch-action":"none","touch-action":"none"})});m(g.prototype,"setDOMEvents",function(a){a.apply(this);
(this.hasZoom||this.followTouchMove)&&this.batchMSEvents(H)});m(g.prototype,"destroy",function(a){this.batchMSEvents(F);a.call(this)})}});K(D,"parts/Legend.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.discardElement,H=g.isNumber,y=g.pick,w=g.setAnimation,x=c.addEvent,E=c.css,F=c.fireEvent;g=c.isFirefox;var t=c.marginNames,m=c.merge,p=c.stableSort,q=c.win,h=c.wrap;c.Legend=function(a,b){this.init(a,b)};c.Legend.prototype={init:function(a,b){this.chart=a;this.setOptions(b);
b.enabled&&(this.render(),x(this.chart,"endResize",function(){this.legend.positionCheckboxes()}),this.proximate?this.unchartrender=x(this.chart,"render",function(){this.legend.proximatePositions();this.legend.positionItems()}):this.unchartrender&&this.unchartrender())},setOptions:function(a){var b=y(a.padding,8);this.options=a;this.chart.styledMode||(this.itemStyle=a.itemStyle,this.itemHiddenStyle=m(this.itemStyle,a.itemHiddenStyle));this.itemMarginTop=a.itemMarginTop||0;this.itemMarginBottom=a.itemMarginBottom||
0;this.padding=b;this.initialItemY=b-5;this.symbolWidth=y(a.symbolWidth,16);this.pages=[];this.proximate="proximate"===a.layout&&!this.chart.inverted},update:function(a,b){var d=this.chart;this.setOptions(m(!0,this.options,a));this.destroy();d.isDirtyLegend=d.isDirtyBox=!0;y(b,!0)&&d.redraw();F(this,"afterUpdate")},colorizeItem:function(a,b){a.legendGroup[b?"removeClass":"addClass"]("highcharts-legend-item-hidden");if(!this.chart.styledMode){var d=this.options,c=a.legendItem,k=a.legendLine,h=a.legendSymbol,
m=this.itemHiddenStyle.color;d=b?d.itemStyle.color:m;var r=b?a.color||m:m,n=a.options&&a.options.marker,f={fill:r};c&&c.css({fill:d,color:d});k&&k.attr({stroke:r});h&&(n&&h.isMarker&&(f=a.pointAttribs(),b||(f.stroke=f.fill=m)),h.attr(f))}F(this,"afterColorizeItem",{item:a,visible:b})},positionItems:function(){this.allItems.forEach(this.positionItem,this);this.chart.isResizing||this.positionCheckboxes()},positionItem:function(a){var b=this.options,d=b.symbolPadding;b=!b.rtl;var c=a._legendItemPos,
k=c[0];c=c[1];var h=a.checkbox;if((a=a.legendGroup)&&a.element)a[I(a.translateY)?"animate":"attr"]({translateX:b?k:this.legendWidth-k-2*d-4,translateY:c});h&&(h.x=k,h.y=c)},destroyItem:function(a){var b=a.checkbox;["legendItem","legendLine","legendSymbol","legendGroup"].forEach(function(b){a[b]&&(a[b]=a[b].destroy())});b&&G(a.checkbox)},destroy:function(){function a(a){this[a]&&(this[a]=this[a].destroy())}this.getAllItems().forEach(function(b){["legendItem","legendGroup"].forEach(a,b)});"clipRect up down pager nav box title group".split(" ").forEach(a,
this);this.display=null},positionCheckboxes:function(){var a=this.group&&this.group.alignAttr,b=this.clipHeight||this.legendHeight,d=this.titleHeight;if(a){var c=a.translateY;this.allItems.forEach(function(e){var k=e.checkbox;if(k){var h=c+d+k.y+(this.scrollOffset||0)+3;E(k,{left:a.translateX+e.checkboxOffset+k.x-20+"px",top:h+"px",display:this.proximate||h>c-6&&h<c+b-6?"":"none"})}},this)}},renderTitle:function(){var a=this.options,b=this.padding,d=a.title,c=0;d.text&&(this.title||(this.title=this.chart.renderer.label(d.text,
b-3,b-4,null,null,null,a.useHTML,null,"legend-title").attr({zIndex:1}),this.chart.styledMode||this.title.css(d.style),this.title.add(this.group)),d.width||this.title.css({width:this.maxLegendWidth+"px"}),a=this.title.getBBox(),c=a.height,this.offsetWidth=a.width,this.contentGroup.attr({translateY:c}));this.titleHeight=c},setText:function(a){var b=this.options;a.legendItem.attr({text:b.labelFormat?c.format(b.labelFormat,a,this.chart.time):b.labelFormatter.call(a)})},renderItem:function(a){var b=this.chart,
d=b.renderer,c=this.options,k=this.symbolWidth,h=c.symbolPadding,g=this.itemStyle,r=this.itemHiddenStyle,n="horizontal"===c.layout?y(c.itemDistance,20):0,f=!c.rtl,l=a.legendItem,v=!a.series,B=!v&&a.series.drawLegendSymbol?a.series:a,A=B.options;A=this.createCheckboxForItem&&A&&A.showCheckbox;n=k+h+n+(A?20:0);var u=c.useHTML,p=a.options.className;l||(a.legendGroup=d.g("legend-item").addClass("highcharts-"+B.type+"-series highcharts-color-"+a.colorIndex+(p?" "+p:"")+(v?" highcharts-series-"+a.index:
"")).attr({zIndex:1}).add(this.scrollGroup),a.legendItem=l=d.text("",f?k+h:-h,this.baseline||0,u),b.styledMode||l.css(m(a.visible?g:r)),l.attr({align:f?"left":"right",zIndex:2}).add(a.legendGroup),this.baseline||(this.fontMetrics=d.fontMetrics(b.styledMode?12:g.fontSize,l),this.baseline=this.fontMetrics.f+3+this.itemMarginTop,l.attr("y",this.baseline)),this.symbolHeight=c.symbolHeight||this.fontMetrics.f,B.drawLegendSymbol(this,a),this.setItemEvents&&this.setItemEvents(a,l,u));A&&!a.checkbox&&this.createCheckboxForItem(a);
this.colorizeItem(a,a.visible);!b.styledMode&&g.width||l.css({width:(c.itemWidth||this.widthOption||b.spacingBox.width)-n});this.setText(a);b=l.getBBox();a.itemWidth=a.checkboxOffset=c.itemWidth||a.legendItemWidth||b.width+n;this.maxItemWidth=Math.max(this.maxItemWidth,a.itemWidth);this.totalItemWidth+=a.itemWidth;this.itemHeight=a.itemHeight=Math.round(a.legendItemHeight||b.height||this.symbolHeight)},layoutItem:function(a){var b=this.options,d=this.padding,c="horizontal"===b.layout,k=a.itemHeight,
h=this.itemMarginBottom,m=this.itemMarginTop,r=c?y(b.itemDistance,20):0,n=this.maxLegendWidth;b=b.alignColumns&&this.totalItemWidth>n?this.maxItemWidth:a.itemWidth;c&&this.itemX-d+b>n&&(this.itemX=d,this.lastLineHeight&&(this.itemY+=m+this.lastLineHeight+h),this.lastLineHeight=0);this.lastItemY=m+this.itemY+h;this.lastLineHeight=Math.max(k,this.lastLineHeight);a._legendItemPos=[this.itemX,this.itemY];c?this.itemX+=b:(this.itemY+=m+k+h,this.lastLineHeight=k);this.offsetWidth=this.widthOption||Math.max((c?
this.itemX-d-(a.checkbox?0:r):b)+d,this.offsetWidth)},getAllItems:function(){var a=[];this.chart.series.forEach(function(b){var d=b&&b.options;b&&y(d.showInLegend,I(d.linkedTo)?!1:void 0,!0)&&(a=a.concat(b.legendItems||("point"===d.legendType?b.data:b)))});F(this,"afterGetAllItems",{allItems:a});return a},getAlignment:function(){var a=this.options;return this.proximate?a.align.charAt(0)+"tv":a.floating?"":a.align.charAt(0)+a.verticalAlign.charAt(0)+a.layout.charAt(0)},adjustMargins:function(a,b){var d=
this.chart,c=this.options,k=this.getAlignment();k&&[/(lth|ct|rth)/,/(rtv|rm|rbv)/,/(rbh|cb|lbh)/,/(lbv|lm|ltv)/].forEach(function(e,h){e.test(k)&&!I(a[h])&&(d[t[h]]=Math.max(d[t[h]],d.legend[(h+1)%2?"legendHeight":"legendWidth"]+[1,-1,-1,1][h]*c[h%2?"x":"y"]+y(c.margin,12)+b[h]+(d.titleOffset[h]||0)))})},proximatePositions:function(){var a=this.chart,b=[],d="left"===this.options.align;this.allItems.forEach(function(e){var k=d;if(e.yAxis&&e.points){e.xAxis.options.reversed&&(k=!k);var h=c.find(k?e.points:
e.points.slice(0).reverse(),function(a){return H(a.plotY)});k=this.itemMarginTop+e.legendItem.getBBox().height+this.itemMarginBottom;var m=e.yAxis.top-a.plotTop;e.visible?(h=h?h.plotY:e.yAxis.height,h+=m-.3*k):h=m+e.yAxis.height;b.push({target:h,size:k,item:e})}},this);c.distribute(b,a.plotHeight);b.forEach(function(b){b.item._legendItemPos[1]=a.plotTop-a.spacing[0]+b.pos})},render:function(){var a=this.chart,b=a.renderer,d=this.group,e,k=this.box,h=this.options,g=this.padding;this.itemX=g;this.itemY=
this.initialItemY;this.lastItemY=this.offsetWidth=0;this.widthOption=c.relativeLength(h.width,a.spacingBox.width-g);var r=a.spacingBox.width-2*g-h.x;-1<["rm","lm"].indexOf(this.getAlignment().substring(0,2))&&(r/=2);this.maxLegendWidth=this.widthOption||r;d||(this.group=d=b.g("legend").attr({zIndex:7}).add(),this.contentGroup=b.g().attr({zIndex:1}).add(d),this.scrollGroup=b.g().add(this.contentGroup));this.renderTitle();r=this.getAllItems();p(r,function(a,b){return(a.options&&a.options.legendIndex||
0)-(b.options&&b.options.legendIndex||0)});h.reversed&&r.reverse();this.allItems=r;this.display=e=!!r.length;this.itemHeight=this.totalItemWidth=this.maxItemWidth=this.lastLineHeight=0;r.forEach(this.renderItem,this);r.forEach(this.layoutItem,this);r=(this.widthOption||this.offsetWidth)+g;var n=this.lastItemY+this.lastLineHeight+this.titleHeight;n=this.handleOverflow(n);n+=g;k||(this.box=k=b.rect().addClass("highcharts-legend-box").attr({r:h.borderRadius}).add(d),k.isNew=!0);a.styledMode||k.attr({stroke:h.borderColor,
"stroke-width":h.borderWidth||0,fill:h.backgroundColor||"none"}).shadow(h.shadow);0<r&&0<n&&(k[k.isNew?"attr":"animate"](k.crisp.call({},{x:0,y:0,width:r,height:n},k.strokeWidth())),k.isNew=!1);k[e?"show":"hide"]();a.styledMode&&"none"===d.getStyle("display")&&(r=n=0);this.legendWidth=r;this.legendHeight=n;e&&(b=a.spacingBox,k=b.y,/(lth|ct|rth)/.test(this.getAlignment())&&0<a.titleOffset[0]?k+=a.titleOffset[0]:/(lbh|cb|rbh)/.test(this.getAlignment())&&0<a.titleOffset[2]&&(k-=a.titleOffset[2]),k!==
b.y&&(b=m(b,{y:k})),d.align(m(h,{width:r,height:n,verticalAlign:this.proximate?"top":h.verticalAlign}),!0,b));this.proximate||this.positionItems();F(this,"afterRender")},handleOverflow:function(a){var b=this,d=this.chart,c=d.renderer,k=this.options,h=k.y,m=this.padding;h=d.spacingBox.height+("top"===k.verticalAlign?-h:h)-m;var r=k.maxHeight,n,f=this.clipRect,l=k.navigation,v=y(l.animation,!0),B=l.arrowSize||12,A=this.nav,u=this.pages,g,p=this.allItems,q=function(a){"number"===typeof a?f.attr({height:a}):
f&&(b.clipRect=f.destroy(),b.contentGroup.clip());b.contentGroup.div&&(b.contentGroup.div.style.clip=a?"rect("+m+"px,9999px,"+(m+a)+"px,0)":"auto")},t=function(a){b[a]=c.circle(0,0,1.3*B).translate(B/2,B/2).add(A);d.styledMode||b[a].attr("fill","rgba(0,0,0,0.0001)");return b[a]};"horizontal"!==k.layout||"middle"===k.verticalAlign||k.floating||(h/=2);r&&(h=Math.min(h,r));u.length=0;a>h&&!1!==l.enabled?(this.clipHeight=n=Math.max(h-20-this.titleHeight-m,0),this.currentPage=y(this.currentPage,1),this.fullHeight=
a,p.forEach(function(a,b){var f=a._legendItemPos[1],l=Math.round(a.legendItem.getBBox().height),d=u.length;if(!d||f-u[d-1]>n&&(g||f)!==u[d-1])u.push(g||f),d++;a.pageIx=d-1;g&&(p[b-1].pageIx=d-1);b===p.length-1&&f+l-u[d-1]>n&&f!==g&&(u.push(f),a.pageIx=d);f!==g&&(g=f)}),f||(f=b.clipRect=c.clipRect(0,m,9999,0),b.contentGroup.clip(f)),q(n),A||(this.nav=A=c.g().attr({zIndex:1}).add(this.group),this.up=c.symbol("triangle",0,0,B,B).add(A),t("upTracker").on("click",function(){b.scroll(-1,v)}),this.pager=
c.text("",15,10).addClass("highcharts-legend-navigation"),d.styledMode||this.pager.css(l.style),this.pager.add(A),this.down=c.symbol("triangle-down",0,0,B,B).add(A),t("downTracker").on("click",function(){b.scroll(1,v)})),b.scroll(0),a=h):A&&(q(),this.nav=A.destroy(),this.scrollGroup.attr({translateY:1}),this.clipHeight=0);return a},scroll:function(a,b){var d=this.pages,c=d.length,k=this.currentPage+a;a=this.clipHeight;var h=this.options.navigation,m=this.pager,r=this.padding;k>c&&(k=c);0<k&&(void 0!==
b&&w(b,this.chart),this.nav.attr({translateX:r,translateY:a+this.padding+7+this.titleHeight,visibility:"visible"}),[this.up,this.upTracker].forEach(function(a){a.attr({"class":1===k?"highcharts-legend-nav-inactive":"highcharts-legend-nav-active"})}),m.attr({text:k+"/"+c}),[this.down,this.downTracker].forEach(function(a){a.attr({x:18+this.pager.getBBox().width,"class":k===c?"highcharts-legend-nav-inactive":"highcharts-legend-nav-active"})},this),this.chart.styledMode||(this.up.attr({fill:1===k?h.inactiveColor:
h.activeColor}),this.upTracker.css({cursor:1===k?"default":"pointer"}),this.down.attr({fill:k===c?h.inactiveColor:h.activeColor}),this.downTracker.css({cursor:k===c?"default":"pointer"})),this.scrollOffset=-d[k-1]+this.initialItemY,this.scrollGroup.animate({translateY:this.scrollOffset}),this.currentPage=k,this.positionCheckboxes())}};c.LegendSymbolMixin={drawRectangle:function(a,b){var d=a.symbolHeight,c=a.options.squareSymbol;b.legendSymbol=this.chart.renderer.rect(c?(a.symbolWidth-d)/2:0,a.baseline-
d+1,c?d:a.symbolWidth,d,y(a.options.symbolRadius,d/2)).addClass("highcharts-point").attr({zIndex:3}).add(b.legendGroup)},drawLineMarker:function(a){var b=this.options,d=b.marker,c=a.symbolWidth,k=a.symbolHeight,h=k/2,g=this.chart.renderer,r=this.legendGroup;a=a.baseline-Math.round(.3*a.fontMetrics.b);var n={};this.chart.styledMode||(n={"stroke-width":b.lineWidth||0},b.dashStyle&&(n.dashstyle=b.dashStyle));this.legendLine=g.path(["M",0,a,"L",c,a]).addClass("highcharts-graph").attr(n).add(r);d&&!1!==
d.enabled&&c&&(b=Math.min(y(d.radius,h),h),0===this.symbol.indexOf("url")&&(d=m(d,{width:k,height:k}),b=0),this.legendSymbol=d=g.symbol(this.symbol,c/2-b,a-b,2*b,2*b,d).addClass("highcharts-point").add(r),d.isMarker=!0)}};(/Trident\/7\.0/.test(q.navigator&&q.navigator.userAgent)||g)&&h(c.Legend.prototype,"positionItem",function(a,b){var d=this,c=function(){b._legendItemPos&&a.call(d,b)};c();d.bubbleLegend||setTimeout(c)})});K(D,"parts/Chart.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,
g){var I=g.attr,G=g.defined,H=g.discardElement,y=g.erase,w=g.extend,x=g.isArray,E=g.isNumber,F=g.isObject,t=g.isString,m=g.objectEach,p=g.pick,q=g.pInt,h=g.setAnimation,a=g.splat,b=g.syncTimeout,d=c.addEvent,e=c.animate,k=c.animObject,C=c.doc,z=c.Axis,r=c.createElement,n=c.defaultOptions,f=c.charts,l=c.css,v=c.find,B=c.fireEvent,A=c.Legend,u=c.marginNames,J=c.merge,L=c.Pointer,U=c.removeEvent,Q=c.seriesTypes,P=c.win,M=c.Chart=function(){this.getArgs.apply(this,arguments)};c.chart=function(a,b,f){return new M(a,
b,f)};w(M.prototype,{callbacks:[],getArgs:function(){var a=[].slice.call(arguments);if(t(a[0])||a[0].nodeName)this.renderTo=a.shift();this.init(a[0],a[1])},init:function(a,b){var l,e=a.series,k=a.plotOptions||{};B(this,"init",{args:arguments},function(){a.series=null;l=J(n,a);m(l.plotOptions,function(a,b){F(a)&&(a.tooltip=k[b]&&J(k[b].tooltip)||void 0)});l.tooltip.userOptions=a.chart&&a.chart.forExport&&a.tooltip.userOptions||a.tooltip;l.series=a.series=e;this.userOptions=a;var v=l.chart,h=v.events;
this.margin=[];this.spacing=[];this.bounds={h:{},v:{}};this.labelCollectors=[];this.callback=b;this.isResizing=0;this.options=l;this.axes=[];this.series=[];this.time=a.time&&Object.keys(a.time).length?new c.Time(a.time):c.time;this.styledMode=v.styledMode;this.hasCartesianSeries=v.showAxes;var u=this;u.index=f.length;f.push(u);c.chartCount++;h&&m(h,function(a,b){c.isFunction(a)&&d(u,b,a)});u.xAxis=[];u.yAxis=[];u.pointCount=u.colorCounter=u.symbolCounter=0;B(u,"afterInit");u.firstRender()})},initSeries:function(a){var b=
this.options.chart;b=a.type||b.type||b.defaultSeriesType;var f=Q[b];f||c.error(17,!0,this,{missingModuleFor:b});b=new f;b.init(this,a);return b},orderSeries:function(a){var b=this.series;for(a=a||0;a<b.length;a++)b[a]&&(b[a].index=a,b[a].name=b[a].getName())},isInsidePlot:function(a,b,f){var l=f?b:a;a=f?a:b;return 0<=l&&l<=this.plotWidth&&0<=a&&a<=this.plotHeight},redraw:function(a){B(this,"beforeRedraw");var b=this.axes,f=this.series,l=this.pointer,d=this.legend,c=this.userOptions.legend,e=this.isDirtyLegend,
k=this.hasCartesianSeries,n=this.isDirtyBox,v=this.renderer,u=v.isHidden(),m=[];this.setResponsive&&this.setResponsive(!1);h(a,this);u&&this.temporaryDisplay();this.layOutTitles();for(a=f.length;a--;){var A=f[a];if(A.options.stacking){var r=!0;if(A.isDirty){var g=!0;break}}}if(g)for(a=f.length;a--;)A=f[a],A.options.stacking&&(A.isDirty=!0);f.forEach(function(a){a.isDirty&&("point"===a.options.legendType?(a.updateTotals&&a.updateTotals(),e=!0):c&&(c.labelFormatter||c.labelFormat)&&(e=!0));a.isDirtyData&&
B(a,"updatedData")});e&&d&&d.options.enabled&&(d.render(),this.isDirtyLegend=!1);r&&this.getStacks();k&&b.forEach(function(a){a.updateNames();a.setScale()});this.getMargins();k&&(b.forEach(function(a){a.isDirty&&(n=!0)}),b.forEach(function(a){var b=a.min+","+a.max;a.extKey!==b&&(a.extKey=b,m.push(function(){B(a,"afterSetExtremes",w(a.eventArgs,a.getExtremes()));delete a.eventArgs}));(n||r)&&a.redraw()}));n&&this.drawChartBox();B(this,"predraw");f.forEach(function(a){(n||a.isDirty)&&a.visible&&a.redraw();
a.isDirtyData=!1});l&&l.reset(!0);v.draw();B(this,"redraw");B(this,"render");u&&this.temporaryDisplay(!0);m.forEach(function(a){a.call()})},get:function(a){function b(b){return b.id===a||b.options&&b.options.id===a}var f=this.series,l;var d=v(this.axes,b)||v(this.series,b);for(l=0;!d&&l<f.length;l++)d=v(f[l].points||[],b);return d},getAxes:function(){var b=this,f=this.options,l=f.xAxis=a(f.xAxis||{});f=f.yAxis=a(f.yAxis||{});B(this,"getAxes");l.forEach(function(a,b){a.index=b;a.isX=!0});f.forEach(function(a,
b){a.index=b});l.concat(f).forEach(function(a){new z(b,a)});B(this,"afterGetAxes")},getSelectedPoints:function(){var a=[];this.series.forEach(function(b){a=a.concat((b[b.hasGroupedData?"points":"data"]||[]).filter(function(a){return p(a.selectedStaging,a.selected)}))});return a},getSelectedSeries:function(){return this.series.filter(function(a){return a.selected})},setTitle:function(a,b,f){this.applyDescription("title",a);this.applyDescription("subtitle",b);this.applyDescription("caption",void 0);
this.layOutTitles(f)},applyDescription:function(a,b){var f=this,l="title"===a?{color:"#333333",fontSize:this.options.isStock?"16px":"18px"}:{color:"#666666"};l=this.options[a]=J(!this.styledMode&&{style:l},this.options[a],b);var d=this[a];d&&b&&(this[a]=d=d.destroy());l&&!d&&(d=this.renderer.text(l.text,0,0,l.useHTML).attr({align:l.align,"class":"highcharts-"+a,zIndex:l.zIndex||4}).add(),d.update=function(b){f[{title:"setTitle",subtitle:"setSubtitle",caption:"setCaption"}[a]](b)},this.styledMode||
d.css(l.style),this[a]=d)},layOutTitles:function(a){var b=[0,0,0],f=this.renderer,l=this.spacingBox;["title","subtitle","caption"].forEach(function(a){var d=this[a],c=this.options[a],e=c.verticalAlign||"top";a="title"===a?-3:"top"===e?b[0]+2:0;if(d){if(!this.styledMode)var k=c.style.fontSize;k=f.fontMetrics(k,d).b;d.css({width:(c.width||l.width+(c.widthAdjust||0))+"px"});var n=Math.round(d.getBBox(c.useHTML).height);d.align(w({y:"bottom"===e?k:a+k,height:n},c),!1,"spacingBox");c.floating||("top"===
e?b[0]=Math.ceil(b[0]+n):"bottom"===e&&(b[2]=Math.ceil(b[2]+n)))}},this);b[0]&&"top"===(this.options.title.verticalAlign||"top")&&(b[0]+=this.options.title.margin);b[2]&&"bottom"===this.options.caption.verticalAlign&&(b[2]+=this.options.caption.margin);var d=!this.titleOffset||this.titleOffset.join(",")!==b.join(",");this.titleOffset=b;B(this,"afterLayOutTitles");!this.isDirtyBox&&d&&(this.isDirtyBox=this.isDirtyLegend=d,this.hasRendered&&p(a,!0)&&this.isDirtyBox&&this.redraw())},getChartSize:function(){var a=
this.options.chart,b=a.width;a=a.height;var f=this.renderTo;G(b)||(this.containerWidth=c.getStyle(f,"width"));G(a)||(this.containerHeight=c.getStyle(f,"height"));this.chartWidth=Math.max(0,b||this.containerWidth||600);this.chartHeight=Math.max(0,c.relativeLength(a,this.chartWidth)||(1<this.containerHeight?this.containerHeight:400))},temporaryDisplay:function(a){var b=this.renderTo;if(a)for(;b&&b.style;)b.hcOrigStyle&&(c.css(b,b.hcOrigStyle),delete b.hcOrigStyle),b.hcOrigDetached&&(C.body.removeChild(b),
b.hcOrigDetached=!1),b=b.parentNode;else for(;b&&b.style;){C.body.contains(b)||b.parentNode||(b.hcOrigDetached=!0,C.body.appendChild(b));if("none"===c.getStyle(b,"display",!1)||b.hcOricDetached)b.hcOrigStyle={display:b.style.display,height:b.style.height,overflow:b.style.overflow},a={display:"block",overflow:"hidden"},b!==this.renderTo&&(a.height=0),c.css(b,a),b.offsetWidth||b.style.setProperty("display","block","important");b=b.parentNode;if(b===C.body)break}},setClassName:function(a){this.container.className=
"highcharts-container "+(a||"")},getContainer:function(){var a=this.options,b=a.chart;var d=this.renderTo;var e=c.uniqueKey(),k,n;d||(this.renderTo=d=b.renderTo);t(d)&&(this.renderTo=d=C.getElementById(d));d||c.error(13,!0,this);var v=q(I(d,"data-highcharts-chart"));E(v)&&f[v]&&f[v].hasRendered&&f[v].destroy();I(d,"data-highcharts-chart",this.index);d.innerHTML="";b.skipClone||d.offsetWidth||this.temporaryDisplay();this.getChartSize();v=this.chartWidth;var h=this.chartHeight;l(d,{overflow:"hidden"});
this.styledMode||(k=w({position:"relative",overflow:"hidden",width:v+"px",height:h+"px",textAlign:"left",lineHeight:"normal",zIndex:0,"-webkit-tap-highlight-color":"rgba(0,0,0,0)"},b.style));this.container=d=r("div",{id:e},k,d);this._cursor=d.style.cursor;this.renderer=new (c[b.renderer]||c.Renderer)(d,v,h,null,b.forExport,a.exporting&&a.exporting.allowHTML,this.styledMode);this.setClassName(b.className);if(this.styledMode)for(n in a.defs)this.renderer.definition(a.defs[n]);else this.renderer.setStyle(b.style);
this.renderer.chartIndex=this.index;B(this,"afterGetContainer")},getMargins:function(a){var b=this.spacing,f=this.margin,l=this.titleOffset;this.resetMargins();l[0]&&!G(f[0])&&(this.plotTop=Math.max(this.plotTop,l[0]+b[0]));l[2]&&!G(f[2])&&(this.marginBottom=Math.max(this.marginBottom,l[2]+b[2]));this.legend&&this.legend.display&&this.legend.adjustMargins(f,b);B(this,"getMargins");a||this.getAxisMargins()},getAxisMargins:function(){var a=this,b=a.axisOffset=[0,0,0,0],f=a.colorAxis,l=a.margin,d=function(a){a.forEach(function(a){a.visible&&
a.getOffset()})};a.hasCartesianSeries?d(a.axes):f&&f.length&&d(f);u.forEach(function(f,d){G(l[d])||(a[f]+=b[d])});a.setChartSize()},reflow:function(a){var f=this,l=f.options.chart,d=f.renderTo,e=G(l.width)&&G(l.height),k=l.width||c.getStyle(d,"width");l=l.height||c.getStyle(d,"height");d=a?a.target:P;if(!e&&!f.isPrinting&&k&&l&&(d===P||d===C)){if(k!==f.containerWidth||l!==f.containerHeight)c.clearTimeout(f.reflowTimeout),f.reflowTimeout=b(function(){f.container&&f.setSize(void 0,void 0,!1)},a?100:
0);f.containerWidth=k;f.containerHeight=l}},setReflow:function(a){var b=this;!1===a||this.unbindReflow?!1===a&&this.unbindReflow&&(this.unbindReflow=this.unbindReflow()):(this.unbindReflow=d(P,"resize",function(a){b.options&&b.reflow(a)}),d(this,"destroy",this.unbindReflow))},setSize:function(a,f,d){var c=this,n=c.renderer;c.isResizing+=1;h(d,c);c.oldChartHeight=c.chartHeight;c.oldChartWidth=c.chartWidth;void 0!==a&&(c.options.chart.width=a);void 0!==f&&(c.options.chart.height=f);c.getChartSize();
if(!c.styledMode){var v=n.globalAnimation;(v?e:l)(c.container,{width:c.chartWidth+"px",height:c.chartHeight+"px"},v)}c.setChartSize(!0);n.setSize(c.chartWidth,c.chartHeight,d);c.axes.forEach(function(a){a.isDirty=!0;a.setScale()});c.isDirtyLegend=!0;c.isDirtyBox=!0;c.layOutTitles();c.getMargins();c.redraw(d);c.oldChartHeight=null;B(c,"resize");b(function(){c&&B(c,"endResize",null,function(){--c.isResizing})},k(v).duration||0)},setChartSize:function(a){var b=this.inverted,f=this.renderer,l=this.chartWidth,
d=this.chartHeight,c=this.options.chart,e=this.spacing,k=this.clipOffset,n,v,h,u;this.plotLeft=n=Math.round(this.plotLeft);this.plotTop=v=Math.round(this.plotTop);this.plotWidth=h=Math.max(0,Math.round(l-n-this.marginRight));this.plotHeight=u=Math.max(0,Math.round(d-v-this.marginBottom));this.plotSizeX=b?u:h;this.plotSizeY=b?h:u;this.plotBorderWidth=c.plotBorderWidth||0;this.spacingBox=f.spacingBox={x:e[3],y:e[0],width:l-e[3]-e[1],height:d-e[0]-e[2]};this.plotBox=f.plotBox={x:n,y:v,width:h,height:u};
l=2*Math.floor(this.plotBorderWidth/2);b=Math.ceil(Math.max(l,k[3])/2);f=Math.ceil(Math.max(l,k[0])/2);this.clipBox={x:b,y:f,width:Math.floor(this.plotSizeX-Math.max(l,k[1])/2-b),height:Math.max(0,Math.floor(this.plotSizeY-Math.max(l,k[2])/2-f))};a||this.axes.forEach(function(a){a.setAxisSize();a.setAxisTranslation()});B(this,"afterSetChartSize",{skipAxes:a})},resetMargins:function(){B(this,"resetMargins");var a=this,b=a.options.chart;["margin","spacing"].forEach(function(f){var l=b[f],d=F(l)?l:[l,
l,l,l];["Top","Right","Bottom","Left"].forEach(function(l,c){a[f][c]=p(b[f+l],d[c])})});u.forEach(function(b,f){a[b]=p(a.margin[f],a.spacing[f])});a.axisOffset=[0,0,0,0];a.clipOffset=[0,0,0,0]},drawChartBox:function(){var a=this.options.chart,b=this.renderer,f=this.chartWidth,l=this.chartHeight,d=this.chartBackground,c=this.plotBackground,e=this.plotBorder,k=this.styledMode,n=this.plotBGImage,v=a.backgroundColor,h=a.plotBackgroundColor,u=a.plotBackgroundImage,m,A=this.plotLeft,r=this.plotTop,g=this.plotWidth,
p=this.plotHeight,J=this.plotBox,q=this.clipRect,z=this.clipBox,C="animate";d||(this.chartBackground=d=b.rect().addClass("highcharts-background").add(),C="attr");if(k)var t=m=d.strokeWidth();else{t=a.borderWidth||0;m=t+(a.shadow?8:0);v={fill:v||"none"};if(t||d["stroke-width"])v.stroke=a.borderColor,v["stroke-width"]=t;d.attr(v).shadow(a.shadow)}d[C]({x:m/2,y:m/2,width:f-m-t%2,height:l-m-t%2,r:a.borderRadius});C="animate";c||(C="attr",this.plotBackground=c=b.rect().addClass("highcharts-plot-background").add());
c[C](J);k||(c.attr({fill:h||"none"}).shadow(a.plotShadow),u&&(n?n.animate(J):this.plotBGImage=b.image(u,A,r,g,p).add()));q?q.animate({width:z.width,height:z.height}):this.clipRect=b.clipRect(z);C="animate";e||(C="attr",this.plotBorder=e=b.rect().addClass("highcharts-plot-border").attr({zIndex:1}).add());k||e.attr({stroke:a.plotBorderColor,"stroke-width":a.plotBorderWidth||0,fill:"none"});e[C](e.crisp({x:A,y:r,width:g,height:p},-e.strokeWidth()));this.isDirtyBox=!1;B(this,"afterDrawChartBox")},propFromSeries:function(){var a=
this,b=a.options.chart,f,l=a.options.series,d,c;["inverted","angular","polar"].forEach(function(e){f=Q[b.type||b.defaultSeriesType];c=b[e]||f&&f.prototype[e];for(d=l&&l.length;!c&&d--;)(f=Q[l[d].type])&&f.prototype[e]&&(c=!0);a[e]=c})},linkSeries:function(){var a=this,b=a.series;b.forEach(function(a){a.linkedSeries.length=0});b.forEach(function(b){var f=b.options.linkedTo;t(f)&&(f=":previous"===f?a.series[b.index-1]:a.get(f))&&f.linkedParent!==b&&(f.linkedSeries.push(b),b.linkedParent=f,b.visible=
p(b.options.visible,f.options.visible,b.visible))});B(this,"afterLinkSeries")},renderSeries:function(){this.series.forEach(function(a){a.translate();a.render()})},renderLabels:function(){var a=this,b=a.options.labels;b.items&&b.items.forEach(function(f){var l=w(b.style,f.style),d=q(l.left)+a.plotLeft,c=q(l.top)+a.plotTop+12;delete l.left;delete l.top;a.renderer.text(f.html,d,c).attr({zIndex:2}).css(l).add()})},render:function(){var a=this.axes,b=this.colorAxis,f=this.renderer,l=this.options,d=0,c=
function(a){a.forEach(function(a){a.visible&&a.render()})};this.setTitle();this.legend=new A(this,l.legend);this.getStacks&&this.getStacks();this.getMargins(!0);this.setChartSize();l=this.plotWidth;a.some(function(a){if(a.horiz&&a.visible&&a.options.labels.enabled&&a.series.length)return d=21,!0});var e=this.plotHeight=Math.max(this.plotHeight-d,0);a.forEach(function(a){a.setScale()});this.getAxisMargins();var k=1.1<l/this.plotWidth;var n=1.05<e/this.plotHeight;if(k||n)a.forEach(function(a){(a.horiz&&
k||!a.horiz&&n)&&a.setTickInterval(!0)}),this.getMargins();this.drawChartBox();this.hasCartesianSeries?c(a):b&&b.length&&c(b);this.seriesGroup||(this.seriesGroup=f.g("series-group").attr({zIndex:3}).add());this.renderSeries();this.renderLabels();this.addCredits();this.setResponsive&&this.setResponsive();this.updateContainerScaling();this.hasRendered=!0},addCredits:function(a){var b=this;a=J(!0,this.options.credits,a);a.enabled&&!this.credits&&(this.credits=this.renderer.text(a.text+(this.mapCredits||
""),0,0).addClass("highcharts-credits").on("click",function(){a.href&&(P.location.href=a.href)}).attr({align:a.position.align,zIndex:8}),b.styledMode||this.credits.css(a.style),this.credits.add().align(a.position),this.credits.update=function(a){b.credits=b.credits.destroy();b.addCredits(a)})},updateContainerScaling:function(){var a=this.container;if(a.offsetWidth&&a.offsetHeight&&a.getBoundingClientRect){var b=a.getBoundingClientRect(),f=b.width/a.offsetWidth;a=b.height/a.offsetHeight;1!==f||1!==
a?this.containerScaling={scaleX:f,scaleY:a}:delete this.containerScaling}},destroy:function(){var a=this,b=a.axes,l=a.series,d=a.container,e,k=d&&d.parentNode;B(a,"destroy");a.renderer.forExport?y(f,a):f[a.index]=void 0;c.chartCount--;a.renderTo.removeAttribute("data-highcharts-chart");U(a);for(e=b.length;e--;)b[e]=b[e].destroy();this.scroller&&this.scroller.destroy&&this.scroller.destroy();for(e=l.length;e--;)l[e]=l[e].destroy();"title subtitle chartBackground plotBackground plotBGImage plotBorder seriesGroup clipRect credits pointer rangeSelector legend resetZoomButton tooltip renderer".split(" ").forEach(function(b){var f=
a[b];f&&f.destroy&&(a[b]=f.destroy())});d&&(d.innerHTML="",U(d),k&&H(d));m(a,function(b,f){delete a[f]})},firstRender:function(){var a=this,b=a.options;if(!a.isReadyToRender||a.isReadyToRender()){a.getContainer();a.resetMargins();a.setChartSize();a.propFromSeries();a.getAxes();(x(b.series)?b.series:[]).forEach(function(b){a.initSeries(b)});a.linkSeries();B(a,"beforeRender");L&&(a.pointer=new L(a,b));a.render();if(!a.renderer.imgCount&&a.onload)a.onload();a.temporaryDisplay(!0)}},onload:function(){this.callbacks.concat([this.callback]).forEach(function(a){a&&
void 0!==this.index&&a.apply(this,[this])},this);B(this,"load");B(this,"render");G(this.index)&&this.setReflow(this.options.chart.reflow);this.onload=null}})});K(D,"parts/ScrollablePlotArea.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.pick,G=c.addEvent;g=c.Chart;"";G(g,"afterSetChartSize",function(g){var y=this.options.chart.scrollablePlotArea,w=y&&y.minWidth;y=y&&y.minHeight;if(!this.renderer.forExport){if(w){if(this.scrollablePixelsX=w=Math.max(0,w-this.chartWidth)){this.plotWidth+=
w;this.inverted?(this.clipBox.height+=w,this.plotBox.height+=w):(this.clipBox.width+=w,this.plotBox.width+=w);var x={1:{name:"right",value:w}}}}else y&&(this.scrollablePixelsY=w=Math.max(0,y-this.chartHeight))&&(this.plotHeight+=w,this.inverted?(this.clipBox.width+=w,this.plotBox.width+=w):(this.clipBox.height+=w,this.plotBox.height+=w),x={2:{name:"bottom",value:w}});x&&!g.skipAxes&&this.axes.forEach(function(g){x[g.side]?g.getPlotLinePath=function(){var w=x[g.side].name,t=this[w];this[w]=t-x[g.side].value;
var m=c.Axis.prototype.getPlotLinePath.apply(this,arguments);this[w]=t;return m}:(g.setAxisSize(),g.setAxisTranslation())})}});G(g,"render",function(){this.scrollablePixelsX||this.scrollablePixelsY?(this.setUpScrolling&&this.setUpScrolling(),this.applyFixed()):this.fixedDiv&&this.applyFixed()});g.prototype.setUpScrolling=function(){var g={WebkitOverflowScrolling:"touch",overflowX:"hidden",overflowY:"hidden"};this.scrollablePixelsX&&(g.overflowX="auto");this.scrollablePixelsY&&(g.overflowY="auto");
this.scrollingContainer=c.createElement("div",{className:"highcharts-scrolling"},g,this.renderTo);this.innerContainer=c.createElement("div",{className:"highcharts-inner-container"},null,this.scrollingContainer);this.innerContainer.appendChild(this.container);this.setUpScrolling=null};g.prototype.moveFixedElements=function(){var c=this.container,g=this.fixedRenderer,w=".highcharts-contextbutton .highcharts-credits .highcharts-legend .highcharts-legend-checkbox .highcharts-navigator-series .highcharts-navigator-xaxis .highcharts-navigator-yaxis .highcharts-navigator .highcharts-reset-zoom .highcharts-scrollbar .highcharts-subtitle .highcharts-title".split(" "),
x;this.scrollablePixelsX&&!this.inverted?x=".highcharts-yaxis":this.scrollablePixelsX&&this.inverted?x=".highcharts-xaxis":this.scrollablePixelsY&&!this.inverted?x=".highcharts-xaxis":this.scrollablePixelsY&&this.inverted&&(x=".highcharts-yaxis");w.push(x,x+"-labels");w.forEach(function(x){[].forEach.call(c.querySelectorAll(x),function(c){(c.namespaceURI===g.SVG_NS?g.box:g.box.parentNode).appendChild(c);c.style.pointerEvents="auto"})})};g.prototype.applyFixed=function(){var g,y=!this.fixedDiv,w=this.options.chart.scrollablePlotArea;
y?(this.fixedDiv=c.createElement("div",{className:"highcharts-fixed"},{position:"absolute",overflow:"hidden",pointerEvents:"none",zIndex:2},null,!0),this.renderTo.insertBefore(this.fixedDiv,this.renderTo.firstChild),this.renderTo.style.overflow="visible",this.fixedRenderer=g=new c.Renderer(this.fixedDiv,this.chartWidth,this.chartHeight),this.scrollableMask=g.path().attr({fill:c.color(this.options.chart.backgroundColor||"#fff").setOpacity(I(w.opacity,.85)).get(),zIndex:-1}).addClass("highcharts-scrollable-mask").add(),
this.moveFixedElements(),G(this,"afterShowResetZoom",this.moveFixedElements),G(this,"afterLayOutTitles",this.moveFixedElements)):this.fixedRenderer.setSize(this.chartWidth,this.chartHeight);g=this.chartWidth+(this.scrollablePixelsX||0);var x=this.chartHeight+(this.scrollablePixelsY||0);c.stop(this.container);this.container.style.width=g+"px";this.container.style.height=x+"px";this.renderer.boxWrapper.attr({width:g,height:x,viewBox:[0,0,g,x].join(" ")});this.chartBackground.attr({width:g,height:x});
this.scrollablePixelsY&&(this.scrollingContainer.style.height=this.chartHeight+"px");y&&(w.scrollPositionX&&(this.scrollingContainer.scrollLeft=this.scrollablePixelsX*w.scrollPositionX),w.scrollPositionY&&(this.scrollingContainer.scrollTop=this.scrollablePixelsY*w.scrollPositionY));x=this.axisOffset;y=this.plotTop-x[0]-1;w=this.plotLeft-x[3]-1;g=this.plotTop+this.plotHeight+x[2]+1;x=this.plotLeft+this.plotWidth+x[1]+1;var E=this.plotLeft+this.plotWidth-(this.scrollablePixelsX||0),F=this.plotTop+this.plotHeight-
(this.scrollablePixelsY||0);y=this.scrollablePixelsX?["M",0,y,"L",this.plotLeft-1,y,"L",this.plotLeft-1,g,"L",0,g,"Z","M",E,y,"L",this.chartWidth,y,"L",this.chartWidth,g,"L",E,g,"Z"]:this.scrollablePixelsY?["M",w,0,"L",w,this.plotTop-1,"L",x,this.plotTop-1,"L",x,0,"Z","M",w,F,"L",w,this.chartHeight,"L",x,this.chartHeight,"L",x,F,"Z"]:["M",0,0];"adjustHeight"!==this.redrawTrigger&&this.scrollableMask.attr({d:y})}});K(D,"parts/Point.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=
g.defined,G=g.erase,H=g.extend,y=g.isArray,w=g.isNumber,x=g.isObject,E=g.pick,F,t=c.fireEvent,m=c.format,p=c.uniqueKey,q=c.removeEvent;c.Point=F=function(){};c.Point.prototype={init:function(c,a,b){this.series=c;this.applyOptions(a,b);this.id=I(this.id)?this.id:p();this.resolveColor();c.chart.pointCount++;t(this,"afterInit");return this},resolveColor:function(){var c=this.series;var a=c.chart.options.chart.colorCount;var b=c.chart.styledMode;b||this.options.color||(this.color=c.color);c.options.colorByPoint?
(b||(a=c.options.colors||c.chart.options.colors,this.color=this.color||a[c.colorCounter],a=a.length),b=c.colorCounter,c.colorCounter++,c.colorCounter===a&&(c.colorCounter=0)):b=c.colorIndex;this.colorIndex=E(this.colorIndex,b)},applyOptions:function(c,a){var b=this.series,d=b.options.pointValKey||b.pointValKey;c=F.prototype.optionsToObject.call(this,c);H(this,c);this.options=this.options?H(this.options,c):c;c.group&&delete this.group;c.dataLabels&&delete this.dataLabels;d&&(this.y=this[d]);this.formatPrefix=
(this.isNull=E(this.isValid&&!this.isValid(),null===this.x||!w(this.y)))?"null":"point";this.selected&&(this.state="select");"name"in this&&void 0===a&&b.xAxis&&b.xAxis.hasNames&&(this.x=b.xAxis.nameToX(this));void 0===this.x&&b&&(this.x=void 0===a?b.autoIncrement(this):a);return this},setNestedProperty:function(c,a,b){b.split(".").reduce(function(b,c,k,h){b[c]=h.length-1===k?a:x(b[c],!0)?b[c]:{};return b[c]},c);return c},optionsToObject:function(h){var a={},b=this.series,d=b.options.keys,e=d||b.pointArrayMap||
["y"],k=e.length,m=0,g=0;if(w(h)||null===h)a[e[0]]=h;else if(y(h))for(!d&&h.length>k&&(b=typeof h[0],"string"===b?a.name=h[0]:"number"===b&&(a.x=h[0]),m++);g<k;)d&&void 0===h[m]||(0<e[g].indexOf(".")?c.Point.prototype.setNestedProperty(a,h[m],e[g]):a[e[g]]=h[m]),m++,g++;else"object"===typeof h&&(a=h,h.dataLabels&&(b._hasPointLabels=!0),h.marker&&(b._hasPointMarkers=!0));return a},getClassName:function(){return"highcharts-point"+(this.selected?" highcharts-point-select":"")+(this.negative?" highcharts-negative":
"")+(this.isNull?" highcharts-null-point":"")+(void 0!==this.colorIndex?" highcharts-color-"+this.colorIndex:"")+(this.options.className?" "+this.options.className:"")+(this.zone&&this.zone.className?" "+this.zone.className.replace("highcharts-negative",""):"")},getZone:function(){var c=this.series,a=c.zones;c=c.zoneAxis||"y";var b=0,d;for(d=a[b];this[c]>=d.value;)d=a[++b];this.nonZonedColor||(this.nonZonedColor=this.color);this.color=d&&d.color&&!this.options.color?d.color:this.nonZonedColor;return d},
hasNewShapeType:function(){return this.graphic&&this.graphic.element.nodeName!==this.shapeType},destroy:function(){var c=this.series.chart,a=c.hoverPoints,b;c.pointCount--;a&&(this.setState(),G(a,this),a.length||(c.hoverPoints=null));if(this===c.hoverPoint)this.onMouseOut();if(this.graphic||this.dataLabel||this.dataLabels)q(this),this.destroyElements();this.legendItem&&c.legend.destroyItem(this);for(b in this)this[b]=null},destroyElements:function(c){var a=this,b=[],d;c=c||{graphic:1,dataLabel:1};
c.graphic&&b.push("graphic","shadowGroup");c.dataLabel&&b.push("dataLabel","dataLabelUpper","connector");for(d=b.length;d--;){var e=b[d];a[e]&&(a[e]=a[e].destroy())}["dataLabel","connector"].forEach(function(b){var d=b+"s";c[b]&&a[d]&&(a[d].forEach(function(a){a.element&&a.destroy()}),delete a[d])})},getLabelConfig:function(){return{x:this.category,y:this.y,color:this.color,colorIndex:this.colorIndex,key:this.name||this.category,series:this.series,point:this,percentage:this.percentage,total:this.total||
this.stackTotal}},tooltipFormatter:function(c){var a=this.series,b=a.tooltipOptions,d=E(b.valueDecimals,""),e=b.valuePrefix||"",k=b.valueSuffix||"";a.chart.styledMode&&(c=a.chart.tooltip.styledModeFormat(c));(a.pointArrayMap||["y"]).forEach(function(a){a="{point."+a;if(e||k)c=c.replace(RegExp(a+"}","g"),e+a+"}"+k);c=c.replace(RegExp(a+"}","g"),a+":,."+d+"f}")});return m(c,{point:this,series:this.series},a.chart.time)},firePointEvent:function(c,a,b){var d=this,e=this.series.options;(e.point.events[c]||
d.options&&d.options.events&&d.options.events[c])&&this.importEvents();"click"===c&&e.allowPointSelect&&(b=function(a){d.select&&d.select(null,a.ctrlKey||a.metaKey||a.shiftKey)});t(this,c,a,b)},visible:!0}});K(D,"parts/Series.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.arrayMax,G=g.arrayMin,H=g.defined,y=g.erase,w=g.extend,x=g.isArray,E=g.isNumber,F=g.isString,t=g.objectEach,m=g.pick,p=g.splat,q=g.syncTimeout,h=c.addEvent,a=c.animObject,b=c.correctFloat,d=c.defaultOptions,
e=c.defaultPlotOptions,k=c.fireEvent,C=c.merge,z=c.removeEvent,r=c.SVGElement,n=c.win;c.Series=c.seriesType("line",null,{lineWidth:2,allowPointSelect:!1,showCheckbox:!1,animation:{duration:1E3},events:{},marker:{lineWidth:0,lineColor:"#ffffff",enabledThreshold:2,radius:4,states:{normal:{animation:!0},hover:{animation:{duration:50},enabled:!0,radiusPlus:2,lineWidthPlus:1},select:{fillColor:"#cccccc",lineColor:"#000000",lineWidth:2}}},point:{events:{}},dataLabels:{align:"center",formatter:function(){return null===
this.y?"":c.numberFormat(this.y,-1)},padding:5,style:{fontSize:"11px",fontWeight:"bold",color:"contrast",textOutline:"1px contrast"},verticalAlign:"bottom",x:0,y:0},cropThreshold:300,opacity:1,pointRange:0,softThreshold:!0,states:{normal:{animation:!0},hover:{animation:{duration:50},lineWidthPlus:1,marker:{},halo:{size:10,opacity:.25}},select:{animation:{duration:0}},inactive:{animation:{duration:50},opacity:.2}},stickyTracking:!0,turboThreshold:1E3,findNearestPointBy:"x"},{axisTypes:["xAxis","yAxis"],
coll:"series",colorCounter:0,cropShoulder:1,directTouch:!1,isCartesian:!0,parallelArrays:["x","y"],pointClass:c.Point,requireSorting:!0,sorted:!0,init:function(a,b){k(this,"init",{options:b});var f=this,l=a.series,d;this.eventOptions=this.eventOptions||{};f.chart=a;f.options=b=f.setOptions(b);f.linkedSeries=[];f.bindAxes();w(f,{name:b.name,state:"",visible:!1!==b.visible,selected:!0===b.selected});var e=b.events;t(e,function(a,b){c.isFunction(a)&&f.eventOptions[b]!==a&&(c.isFunction(f.eventOptions[b])&&
z(f,b,f.eventOptions[b]),f.eventOptions[b]=a,h(f,b,a))});if(e&&e.click||b.point&&b.point.events&&b.point.events.click||b.allowPointSelect)a.runTrackerClick=!0;f.getColor();f.getSymbol();f.parallelArrays.forEach(function(a){f[a+"Data"]||(f[a+"Data"]=[])});f.points||f.data||f.setData(b.data,!1);f.isCartesian&&(a.hasCartesianSeries=!0);l.length&&(d=l[l.length-1]);f._i=m(d&&d._i,-1)+1;a.orderSeries(this.insert(l));k(this,"afterInit")},insert:function(a){var b=this.options.index,f;if(E(b)){for(f=a.length;f--;)if(b>=
m(a[f].options.index,a[f]._i)){a.splice(f+1,0,this);break}-1===f&&a.unshift(this);f+=1}else a.push(this);return m(f,a.length-1)},bindAxes:function(){var a=this,b=a.options,d=a.chart,e;k(this,"bindAxes",null,function(){(a.axisTypes||[]).forEach(function(f){d[f].forEach(function(l){e=l.options;if(b[f]===e.index||void 0!==b[f]&&b[f]===e.id||void 0===b[f]&&0===e.index)a.insert(l.series),a[f]=l,l.isDirty=!0});a[f]||a.optionalAxis===f||c.error(18,!0,d)})})},updateParallelArrays:function(a,b){var f=a.series,
l=arguments,c=E(b)?function(l){var c="y"===l&&f.toYData?f.toYData(a):a[l];f[l+"Data"][b]=c}:function(a){Array.prototype[b].apply(f[a+"Data"],Array.prototype.slice.call(l,2))};f.parallelArrays.forEach(c)},hasData:function(){return this.visible&&void 0!==this.dataMax&&void 0!==this.dataMin||this.visible&&this.yData&&0<this.yData.length},autoIncrement:function(){var a=this.options,b=this.xIncrement,c,d=a.pointIntervalUnit,e=this.chart.time;b=m(b,a.pointStart,0);this.pointInterval=c=m(this.pointInterval,
a.pointInterval,1);d&&(a=new e.Date(b),"day"===d?e.set("Date",a,e.get("Date",a)+c):"month"===d?e.set("Month",a,e.get("Month",a)+c):"year"===d&&e.set("FullYear",a,e.get("FullYear",a)+c),c=a.getTime()-b);this.xIncrement=b+c;return b},setOptions:function(a){var b=this.chart,f=b.options,c=f.plotOptions,e=b.userOptions||{};a=C(a);b=b.styledMode;var n={plotOptions:c,userOptions:a};k(this,"setOptions",n);var h=n.plotOptions[this.type],r=e.plotOptions||{};this.userOptions=n.userOptions;e=C(h,c.series,e.plotOptions&&
e.plotOptions[this.type],a);this.tooltipOptions=C(d.tooltip,d.plotOptions.series&&d.plotOptions.series.tooltip,d.plotOptions[this.type].tooltip,f.tooltip.userOptions,c.series&&c.series.tooltip,c[this.type].tooltip,a.tooltip);this.stickyTracking=m(a.stickyTracking,r[this.type]&&r[this.type].stickyTracking,r.series&&r.series.stickyTracking,this.tooltipOptions.shared&&!this.noSharedTooltip?!0:e.stickyTracking);null===h.marker&&delete e.marker;this.zoneAxis=e.zoneAxis;f=this.zones=(e.zones||[]).slice();
!e.negativeColor&&!e.negativeFillColor||e.zones||(c={value:e[this.zoneAxis+"Threshold"]||e.threshold||0,className:"highcharts-negative"},b||(c.color=e.negativeColor,c.fillColor=e.negativeFillColor),f.push(c));f.length&&H(f[f.length-1].value)&&f.push(b?{}:{color:this.color,fillColor:this.fillColor});k(this,"afterSetOptions",{options:e});return e},getName:function(){return m(this.options.name,"Series "+(this.index+1))},getCyclic:function(a,b,c){var f=this.chart,l=this.userOptions,d=a+"Index",e=a+"Counter",
k=c?c.length:m(f.options.chart[a+"Count"],f[a+"Count"]);if(!b){var n=m(l[d],l["_"+d]);H(n)||(f.series.length||(f[e]=0),l["_"+d]=n=f[e]%k,f[e]+=1);c&&(b=c[n])}void 0!==n&&(this[d]=n);this[a]=b},getColor:function(){this.chart.styledMode?this.getCyclic("color"):this.options.colorByPoint?this.options.color=null:this.getCyclic("color",this.options.color||e[this.type].color,this.chart.options.colors)},getSymbol:function(){this.getCyclic("symbol",this.options.marker.symbol,this.chart.options.symbols)},findPointIndex:function(a,
b){var f=a.id;a=a.x;var l=this.points,c;if(f){var d=(f=this.chart.get(f))&&f.index;void 0!==d&&(c=!0)}void 0===d&&E(a)&&(d=this.xData.indexOf(a,b));-1!==d&&void 0!==d&&this.cropped&&(d=d>=this.cropStart?d-this.cropStart:d);!c&&l[d]&&l[d].touched&&(d=void 0);return d},drawLegendSymbol:c.LegendSymbolMixin.drawLineMarker,updateData:function(a){var b=this.options,f=this.points,c=[],d,e,n,k=this.requireSorting,m=a.length===f.length,h=!0;this.xIncrement=null;a.forEach(function(a,l){var e=H(a)&&this.pointClass.prototype.optionsToObject.call({series:this},
a)||{};var v=e.x;if(e.id||E(v))if(v=this.findPointIndex(e,n),-1===v||void 0===v?c.push(a):f[v]&&a!==b.data[v]?(f[v].update(a,!1,null,!1),f[v].touched=!0,k&&(n=v+1)):f[v]&&(f[v].touched=!0),!m||l!==v||this.hasDerivedData)d=!0},this);if(d)for(a=f.length;a--;)(e=f[a])&&!e.touched&&e.remove(!1);else m?a.forEach(function(a,b){f[b].update&&a!==f[b].y&&f[b].update(a,!1,null,!1)}):h=!1;f.forEach(function(a){a&&(a.touched=!1)});if(!h)return!1;c.forEach(function(a){this.addPoint(a,!1,null,null,!1)},this);return!0},
setData:function(a,b,d,e){var f=this,l=f.points,n=l&&l.length||0,k,v=f.options,B=f.chart,h=null,r=f.xAxis;h=v.turboThreshold;var g=this.xData,p=this.yData,q=(k=f.pointArrayMap)&&k.length,t=v.keys,z=0,C=1,w;a=a||[];k=a.length;b=m(b,!0);!1!==e&&k&&n&&!f.cropped&&!f.hasGroupedData&&f.visible&&!f.isSeriesBoosting&&(w=this.updateData(a));if(!w){f.xIncrement=null;f.colorCounter=0;this.parallelArrays.forEach(function(a){f[a+"Data"].length=0});if(h&&k>h)if(h=f.getFirstValidPoint(a),E(h))for(d=0;d<k;d++)g[d]=
this.autoIncrement(),p[d]=a[d];else if(x(h))if(q)for(d=0;d<k;d++)e=a[d],g[d]=e[0],p[d]=e.slice(1,q+1);else for(t&&(z=t.indexOf("x"),C=t.indexOf("y"),z=0<=z?z:0,C=0<=C?C:1),d=0;d<k;d++)e=a[d],g[d]=e[z],p[d]=e[C];else c.error(12,!1,B);else for(d=0;d<k;d++)void 0!==a[d]&&(e={series:f},f.pointClass.prototype.applyOptions.apply(e,[a[d]]),f.updateParallelArrays(e,d));p&&F(p[0])&&c.error(14,!0,B);f.data=[];f.options.data=f.userOptions.data=a;for(d=n;d--;)l[d]&&l[d].destroy&&l[d].destroy();r&&(r.minRange=
r.userMinRange);f.isDirty=B.isDirtyBox=!0;f.isDirtyData=!!l;d=!1}"point"===v.legendType&&(this.processData(),this.generatePoints());b&&B.redraw(d)},processData:function(a){var b=this.xData,f=this.yData,d=b.length;var e=0;var k=this.xAxis,n=this.options;var h=n.cropThreshold;var m=this.getExtremesFromAll||n.getExtremesFromAll,r=this.isCartesian;n=k&&k.val2lin;var g=k&&k.isLog,p=this.requireSorting;if(r&&!this.isDirty&&!k.isDirty&&!this.yAxis.isDirty&&!a)return!1;if(k){a=k.getExtremes();var q=a.min;
var t=a.max}if(r&&this.sorted&&!m&&(!h||d>h||this.forceCrop))if(b[d-1]<q||b[0]>t)b=[],f=[];else if(this.yData&&(b[0]<q||b[d-1]>t)){e=this.cropData(this.xData,this.yData,q,t);b=e.xData;f=e.yData;e=e.start;var z=!0}for(h=b.length||1;--h;)if(d=g?n(b[h])-n(b[h-1]):b[h]-b[h-1],0<d&&(void 0===C||d<C))var C=d;else 0>d&&p&&(c.error(15,!1,this.chart),p=!1);this.cropped=z;this.cropStart=e;this.processedXData=b;this.processedYData=f;this.closestPointRange=this.basePointRange=C},cropData:function(a,b,d,c,e){var f=
a.length,l=0,k=f,n;e=m(e,this.cropShoulder);for(n=0;n<f;n++)if(a[n]>=d){l=Math.max(0,n-e);break}for(d=n;d<f;d++)if(a[d]>c){k=d+e;break}return{xData:a.slice(l,k),yData:b.slice(l,k),start:l,end:k}},generatePoints:function(){var a=this.options,b=a.data,d=this.data,c,e=this.processedXData,n=this.processedYData,h=this.pointClass,m=e.length,r=this.cropStart||0,g=this.hasGroupedData;a=a.keys;var q=[],t;d||g||(d=[],d.length=b.length,d=this.data=d);a&&g&&(this.options.keys=!1);for(t=0;t<m;t++){var z=r+t;if(g){var C=
(new h).init(this,[e[t]].concat(p(n[t])));C.dataGroup=this.groupMap[t];C.dataGroup.options&&(C.options=C.dataGroup.options,w(C,C.dataGroup.options),delete C.dataLabels)}else(C=d[z])||void 0===b[z]||(d[z]=C=(new h).init(this,b[z],e[t]));C&&(C.index=z,q[t]=C)}this.options.keys=a;if(d&&(m!==(c=d.length)||g))for(t=0;t<c;t++)t!==r||g||(t+=m),d[t]&&(d[t].destroyElements(),d[t].plotX=void 0);this.data=d;this.points=q;k(this,"afterGeneratePoints")},getXExtremes:function(a){return{min:G(a),max:I(a)}},getExtremes:function(a){var b=
this.xAxis,f=this.yAxis,d=this.processedXData||this.xData,c=[],e=0,n=0;var h=0;var m=this.requireSorting?this.cropShoulder:0,r=f?f.positiveValuesOnly:!1,g;a=a||this.stackedYData||this.processedYData||[];f=a.length;b&&(h=b.getExtremes(),n=h.min,h=h.max);for(g=0;g<f;g++){var p=d[g];var t=a[g];var q=(E(t)||x(t))&&(t.length||0<t||!r);p=this.getExtremesFromAll||this.options.getExtremesFromAll||this.cropped||!b||(d[g+m]||p)>=n&&(d[g-m]||p)<=h;if(q&&p)if(q=t.length)for(;q--;)E(t[q])&&(c[e++]=t[q]);else c[e++]=
t}this.dataMin=G(c);this.dataMax=I(c);k(this,"afterGetExtremes")},getFirstValidPoint:function(a){for(var b=null,f=a.length,d=0;null===b&&d<f;)b=a[d],d++;return b},translate:function(){this.processedXData||this.processData();this.generatePoints();var a=this.options,d=a.stacking,c=this.xAxis,e=c.categories,n=this.yAxis,h=this.points,r=h.length,g=!!this.modifyValue,p,t=this.pointPlacementToXValue(),q=E(t),C=a.threshold,z=a.startFromThreshold?C:0,w,y=this.zoneAxis||"y",F=Number.MAX_VALUE;for(p=0;p<r;p++){var G=
h[p],I=G.x;var D=G.y;var T=G.low,K=d&&n.stacks[(this.negStacks&&D<(z?0:C)?"-":"")+this.stackKey];n.positiveValuesOnly&&null!==D&&0>=D&&(G.isNull=!0);G.plotX=w=b(Math.min(Math.max(-1E5,c.translate(I,0,0,0,1,t,"flags"===this.type)),1E5));if(d&&this.visible&&K&&K[I]){var Z=this.getStackIndicator(Z,I,this.index);if(!G.isNull){var aa=K[I];var V=aa.points[Z.key]}}x(V)&&(T=V[0],D=V[1],T===z&&Z.key===K[I].base&&(T=m(E(C)&&C,n.min)),n.positiveValuesOnly&&0>=T&&(T=null),G.total=G.stackTotal=aa.total,G.percentage=
aa.total&&G.y/aa.total*100,G.stackY=D,this.irregularWidths||aa.setOffset(this.pointXOffset||0,this.barW||0));G.yBottom=H(T)?Math.min(Math.max(-1E5,n.translate(T,0,1,0,1)),1E5):null;g&&(D=this.modifyValue(D,G));G.plotY=D="number"===typeof D&&Infinity!==D?Math.min(Math.max(-1E5,n.translate(D,0,1,0,1)),1E5):void 0;G.isInside=void 0!==D&&0<=D&&D<=n.len&&0<=w&&w<=c.len;G.clientX=q?b(c.translate(I,0,0,0,1,t)):w;G.negative=G[y]<(a[y+"Threshold"]||C||0);G.category=e&&void 0!==e[G.x]?e[G.x]:G.x;if(!G.isNull){void 0!==
W&&(F=Math.min(F,Math.abs(w-W)));var W=w}G.zone=this.zones.length&&G.getZone()}this.closestPointRangePx=F;k(this,"afterTranslate")},getValidPoints:function(a,b,d){var f=this.chart;return(a||this.points||[]).filter(function(a){return b&&!f.isInsidePlot(a.plotX,a.plotY,f.inverted)?!1:d||!a.isNull})},getClipBox:function(a,b){var f=this.options,d=this.chart,l=d.inverted,c=this.xAxis,e=c&&this.yAxis;a&&!1===f.clip&&e?a=l?{y:-d.chartWidth+e.len+e.pos,height:d.chartWidth,width:d.chartHeight,x:-d.chartHeight+
c.len+c.pos}:{y:-e.pos,height:d.chartHeight,width:d.chartWidth,x:-c.pos}:(a=this.clipBox||d.clipBox,b&&(a.width=d.plotSizeX,a.x=0));return b?{width:a.width,x:a.x}:a},setClip:function(a){var b=this.chart,f=this.options,d=b.renderer,c=b.inverted,e=this.clipBox,n=this.getClipBox(a),k=this.sharedClipKey||["_sharedClip",a&&a.duration,a&&a.easing,n.height,f.xAxis,f.yAxis].join(),h=b[k],m=b[k+"m"];h||(a&&(n.width=0,c&&(n.x=b.plotSizeX+(!1!==f.clip?0:b.plotTop)),b[k+"m"]=m=d.clipRect(c?b.plotSizeX+99:-99,
c?-b.plotLeft:-b.plotTop,99,c?b.chartWidth:b.chartHeight)),b[k]=h=d.clipRect(n),h.count={length:0});a&&!h.count[this.index]&&(h.count[this.index]=!0,h.count.length+=1);if(!1!==f.clip||a)this.group.clip(a||e?h:b.clipRect),this.markerGroup.clip(m),this.sharedClipKey=k;a||(h.count[this.index]&&(delete h.count[this.index],--h.count.length),0===h.count.length&&k&&b[k]&&(e||(b[k]=b[k].destroy()),b[k+"m"]&&(b[k+"m"]=b[k+"m"].destroy())))},animate:function(b){var f=this.chart,d=a(this.options.animation);
if(b)this.setClip(d);else{var c=this.sharedClipKey;b=f[c];var e=this.getClipBox(d,!0);b&&b.animate(e,d);f[c+"m"]&&f[c+"m"].animate({width:e.width+99,x:e.x-(f.inverted?0:99)},d);this.animate=null}},afterAnimate:function(){this.setClip();k(this,"afterAnimate");this.finishedAnimating=!0},drawPoints:function(){var a=this.points,b=this.chart,d,c=this.options.marker,e=this[this.specialGroup]||this.markerGroup;var n=this.xAxis;var k=m(c.enabled,!n||n.isRadial?!0:null,this.closestPointRangePx>=c.enabledThreshold*
c.radius);if(!1!==c.enabled||this._hasPointMarkers)for(n=0;n<a.length;n++){var h=a[n];var r=(d=h.graphic)?"animate":"attr";var g=h.marker||{};var p=!!h.marker;var t=k&&void 0===g.enabled||g.enabled;var q=!1!==h.isInside;if(t&&!h.isNull){var C=m(g.symbol,this.symbol);t=this.markerAttribs(h,h.selected&&"select");d?d[q?"show":"hide"](q).animate(t):q&&(0<t.width||h.hasImage)&&(h.graphic=d=b.renderer.symbol(C,t.x,t.y,t.width,t.height,p?g:c).add(e));if(d&&!b.styledMode)d[r](this.pointAttribs(h,h.selected&&
"select"));d&&d.addClass(h.getClassName(),!0)}else d&&(h.graphic=d.destroy())}},markerAttribs:function(a,b){var f=this.options.marker,d=a.marker||{},c=d.symbol||f.symbol,l=m(d.radius,f.radius);b&&(f=f.states[b],b=d.states&&d.states[b],l=m(b&&b.radius,f&&f.radius,l+(f&&f.radiusPlus||0)));a.hasImage=c&&0===c.indexOf("url");a.hasImage&&(l=0);a={x:Math.floor(a.plotX)-l,y:a.plotY-l};l&&(a.width=a.height=2*l);return a},pointAttribs:function(a,b){var f=this.options.marker,d=a&&a.options,c=d&&d.marker||{},
l=this.color,e=d&&d.color,n=a&&a.color;d=m(c.lineWidth,f.lineWidth);var k=a&&a.zone&&a.zone.color;a=1;l=e||k||n||l;e=c.fillColor||f.fillColor||l;l=c.lineColor||f.lineColor||l;b=b||"normal";f=f.states[b];b=c.states&&c.states[b]||{};d=m(b.lineWidth,f.lineWidth,d+m(b.lineWidthPlus,f.lineWidthPlus,0));e=b.fillColor||f.fillColor||e;l=b.lineColor||f.lineColor||l;a=m(b.opacity,f.opacity,a);return{stroke:l,"stroke-width":d,fill:e,opacity:a}},destroy:function(a){var b=this,f=b.chart,d=/AppleWebKit\/533/.test(n.navigator.userAgent),
e,h,m=b.data||[],g,p;k(b,"destroy");a||z(b);(b.axisTypes||[]).forEach(function(a){(p=b[a])&&p.series&&(y(p.series,b),p.isDirty=p.forceRedraw=!0)});b.legendItem&&b.chart.legend.destroyItem(b);for(h=m.length;h--;)(g=m[h])&&g.destroy&&g.destroy();b.points=null;c.clearTimeout(b.animationTimeout);t(b,function(a,b){a instanceof r&&!a.survive&&(e=d&&"group"===b?"hide":"destroy",a[e]())});f.hoverSeries===b&&(f.hoverSeries=null);y(f.series,b);f.orderSeries();t(b,function(f,d){a&&"hcEvents"===d||delete b[d]})},
getGraphPath:function(a,b,d){var f=this,c=f.options,l=c.step,e,n=[],k=[],h;a=a||f.points;(e=a.reversed)&&a.reverse();(l={right:1,center:2}[l]||l&&3)&&e&&(l=4-l);!c.connectNulls||b||d||(a=this.getValidPoints(a));a.forEach(function(e,m){var B=e.plotX,v=e.plotY,r=a[m-1];(e.leftCliff||r&&r.rightCliff)&&!d&&(h=!0);e.isNull&&!H(b)&&0<m?h=!c.connectNulls:e.isNull&&!b?h=!0:(0===m||h?m=["M",e.plotX,e.plotY]:f.getPointSpline?m=f.getPointSpline(a,e,m):l?(m=1===l?["L",r.plotX,v]:2===l?["L",(r.plotX+B)/2,r.plotY,
"L",(r.plotX+B)/2,v]:["L",B,r.plotY],m.push("L",B,v)):m=["L",B,v],k.push(e.x),l&&(k.push(e.x),2===l&&k.push(e.x)),n.push.apply(n,m),h=!1)});n.xMap=k;return f.graphPath=n},drawGraph:function(){var a=this,b=this.options,d=(this.gappedPath||this.getGraphPath).call(this),c=this.chart.styledMode,e=[["graph","highcharts-graph"]];c||e[0].push(b.lineColor||this.color||"#cccccc",b.dashStyle);e=a.getZonesGraphs(e);e.forEach(function(f,l){var e=f[0],n=a[e],k=n?"animate":"attr";n?(n.endX=a.preventGraphAnimation?
null:d.xMap,n.animate({d:d})):d.length&&(a[e]=n=a.chart.renderer.path(d).addClass(f[1]).attr({zIndex:1}).add(a.group));n&&!c&&(e={stroke:f[2],"stroke-width":b.lineWidth,fill:a.fillGraph&&a.color||"none"},f[3]?e.dashstyle=f[3]:"square"!==b.linecap&&(e["stroke-linecap"]=e["stroke-linejoin"]="round"),n[k](e).shadow(2>l&&b.shadow));n&&(n.startX=d.xMap,n.isArea=d.isArea)})},getZonesGraphs:function(a){this.zones.forEach(function(b,f){f=["zone-graph-"+f,"highcharts-graph highcharts-zone-graph-"+f+" "+(b.className||
"")];this.chart.styledMode||f.push(b.color||this.color,b.dashStyle||this.options.dashStyle);a.push(f)},this);return a},applyZones:function(){var a=this,b=this.chart,d=b.renderer,c=this.zones,e,n,k=this.clips||[],h,r=this.graph,g=this.area,p=Math.max(b.chartWidth,b.chartHeight),t=this[(this.zoneAxis||"y")+"Axis"],q=b.inverted,C,z,x,w=!1;if(c.length&&(r||g)&&t&&void 0!==t.min){var E=t.reversed;var y=t.horiz;r&&!this.showLine&&r.hide();g&&g.hide();var F=t.getExtremes();c.forEach(function(f,c){e=E?y?
b.plotWidth:0:y?0:t.toPixels(F.min)||0;e=Math.min(Math.max(m(n,e),0),p);n=Math.min(Math.max(Math.round(t.toPixels(m(f.value,F.max),!0)||0),0),p);w&&(e=n=t.toPixels(F.max));C=Math.abs(e-n);z=Math.min(e,n);x=Math.max(e,n);t.isXAxis?(h={x:q?x:z,y:0,width:C,height:p},y||(h.x=b.plotHeight-h.x)):(h={x:0,y:q?x:z,width:p,height:C},y&&(h.y=b.plotWidth-h.y));q&&d.isVML&&(h=t.isXAxis?{x:0,y:E?z:x,height:h.width,width:b.chartWidth}:{x:h.y-b.plotLeft-b.spacingBox.x,y:0,width:h.height,height:b.chartHeight});k[c]?
k[c].animate(h):k[c]=d.clipRect(h);r&&a["zone-graph-"+c].clip(k[c]);g&&a["zone-area-"+c].clip(k[c]);w=f.value>F.max;a.resetZones&&0===n&&(n=void 0)});this.clips=k}else a.visible&&(r&&r.show(!0),g&&g.show(!0))},invertGroups:function(a){function b(){["group","markerGroup"].forEach(function(b){f[b]&&(d.renderer.isVML&&f[b].attr({width:f.yAxis.len,height:f.xAxis.len}),f[b].width=f.yAxis.len,f[b].height=f.xAxis.len,f[b].invert(a))})}var f=this,d=f.chart;if(f.xAxis){var c=h(d,"resize",b);h(f,"destroy",
c);b(a);f.invertGroups=b}},plotGroup:function(a,b,d,c,e){var f=this[a],l=!f;l&&(this[a]=f=this.chart.renderer.g().attr({zIndex:c||.1}).add(e));f.addClass("highcharts-"+b+" highcharts-series-"+this.index+" highcharts-"+this.type+"-series "+(H(this.colorIndex)?"highcharts-color-"+this.colorIndex+" ":"")+(this.options.className||"")+(f.hasClass("highcharts-tracker")?" highcharts-tracker":""),!0);f.attr({visibility:d})[l?"attr":"animate"](this.getPlotBox());return f},getPlotBox:function(){var a=this.chart,
b=this.xAxis,d=this.yAxis;a.inverted&&(b=d,d=this.xAxis);return{translateX:b?b.left:a.plotLeft,translateY:d?d.top:a.plotTop,scaleX:1,scaleY:1}},render:function(){var b=this,d=b.chart,c=b.options,e=!!b.animate&&d.renderer.isSVG&&a(c.animation).duration,n=b.visible?"inherit":"hidden",h=c.zIndex,m=b.hasRendered,r=d.seriesGroup,g=d.inverted;k(this,"render");var p=b.plotGroup("group","series",n,h,r);b.markerGroup=b.plotGroup("markerGroup","markers",n,h,r);e&&b.animate(!0);p.inverted=b.isCartesian||b.invertable?
g:!1;b.drawGraph&&(b.drawGraph(),b.applyZones());b.visible&&b.drawPoints();b.drawDataLabels&&b.drawDataLabels();b.redrawPoints&&b.redrawPoints();b.drawTracker&&!1!==b.options.enableMouseTracking&&b.drawTracker();b.invertGroups(g);!1===c.clip||b.sharedClipKey||m||p.clip(d.clipRect);e&&b.animate();m||(b.animationTimeout=q(function(){b.afterAnimate()},e||0));b.isDirty=!1;b.hasRendered=!0;k(b,"afterRender")},redraw:function(){var a=this.chart,b=this.isDirty||this.isDirtyData,d=this.group,c=this.xAxis,
e=this.yAxis;d&&(a.inverted&&d.attr({width:a.plotWidth,height:a.plotHeight}),d.animate({translateX:m(c&&c.left,a.plotLeft),translateY:m(e&&e.top,a.plotTop)}));this.translate();this.render();b&&delete this.kdTree},kdAxisArray:["clientX","plotY"],searchPoint:function(a,b){var f=this.xAxis,d=this.yAxis,c=this.chart.inverted;return this.searchKDTree({clientX:c?f.len-a.chartY+f.pos:a.chartX-f.pos,plotY:c?d.len-a.chartX+d.pos:a.chartY-d.pos},b,a)},buildKDTree:function(a){function b(a,d,c){var l;if(l=a&&
a.length){var e=f.kdAxisArray[d%c];a.sort(function(a,b){return a[e]-b[e]});l=Math.floor(l/2);return{point:a[l],left:b(a.slice(0,l),d+1,c),right:b(a.slice(l+1),d+1,c)}}}this.buildingKdTree=!0;var f=this,d=-1<f.options.findNearestPointBy.indexOf("y")?2:1;delete f.kdTree;q(function(){f.kdTree=b(f.getValidPoints(null,!f.directTouch),d,d);f.buildingKdTree=!1},f.options.kdNow||a&&"touchstart"===a.type?0:1)},searchKDTree:function(a,b,d){function f(a,b,d,k){var h=b.point,m=c.kdAxisArray[d%k],r=h;var B=H(a[l])&&
H(h[l])?Math.pow(a[l]-h[l],2):null;var g=H(a[e])&&H(h[e])?Math.pow(a[e]-h[e],2):null;g=(B||0)+(g||0);h.dist=H(g)?Math.sqrt(g):Number.MAX_VALUE;h.distX=H(B)?Math.sqrt(B):Number.MAX_VALUE;m=a[m]-h[m];g=0>m?"left":"right";B=0>m?"right":"left";b[g]&&(g=f(a,b[g],d+1,k),r=g[n]<r[n]?g:h);b[B]&&Math.sqrt(m*m)<r[n]&&(a=f(a,b[B],d+1,k),r=a[n]<r[n]?a:r);return r}var c=this,l=this.kdAxisArray[0],e=this.kdAxisArray[1],n=b?"distX":"dist";b=-1<c.options.findNearestPointBy.indexOf("y")?2:1;this.kdTree||this.buildingKdTree||
this.buildKDTree(d);if(this.kdTree)return f(a,this.kdTree,b,b)},pointPlacementToXValue:function(){var a=this.xAxis,b=this.options.pointPlacement;"between"===b&&(b=a.reversed?-.5:.5);E(b)&&(b*=m(this.options.pointRange||a.pointRange));return b}});""});K(D,"parts/Stacking.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.destroyObjectProperties,H=g.objectEach,y=g.pick;g=c.Axis;var w=c.Chart,x=c.correctFloat,E=c.format,F=c.Series;c.StackItem=function(c,m,g,q,h){var a=
c.chart.inverted;this.axis=c;this.isNegative=g;this.options=m=m||{};this.x=q;this.total=null;this.points={};this.stack=h;this.rightCliff=this.leftCliff=0;this.alignOptions={align:m.align||(a?g?"left":"right":"center"),verticalAlign:m.verticalAlign||(a?"middle":g?"bottom":"top"),y:m.y,x:m.x};this.textAlign=m.textAlign||(a?g?"right":"left":"center")};c.StackItem.prototype={destroy:function(){G(this,this.axis)},render:function(c){var m=this.axis.chart,g=this.options,t=g.format;t=t?E(t,this,m.time):g.formatter.call(this);
this.label?this.label.attr({text:t,visibility:"hidden"}):(this.label=m.renderer.label(t,null,null,g.shape,null,null,g.useHTML,!1,"stack-labels"),t={text:t,align:this.textAlign,rotation:g.rotation,padding:y(g.padding,0),visibility:"hidden"},this.label.attr(t),m.styledMode||this.label.css(g.style),this.label.added||this.label.add(c));this.label.labelrank=m.plotHeight},setOffset:function(c,m,g,q,h){var a=this.axis,b=a.chart;q=a.translate(a.usePercentage?100:q?q:this.total,0,0,0,1);g=a.translate(g?g:
0);g=I(q)&&Math.abs(q-g);c=y(h,b.xAxis[0].translate(this.x))+c;a=I(q)&&this.getStackBox(b,this,c,q,m,g,a);m=this.label;c=this.isNegative;h="justify"===y(this.options.overflow,"justify");if(m&&a){g=m.getBBox();var d=b.inverted?c?g.width:0:g.width/2,e=b.inverted?g.height/2:c?-4:g.height+4;this.alignOptions.x=y(this.options.x,0);m.align(this.alignOptions,null,a);q=m.alignAttr;m.show();q.y-=e;h&&(q.x-=d,F.prototype.justifyDataLabel.call(this.axis,m,this.alignOptions,q,g,a),q.x+=d);q.x=m.alignAttr.x;m.attr({x:q.x,
y:q.y});y(!h&&this.options.crop,!0)&&((b=b.isInsidePlot(m.x+(b.inverted?0:-g.width/2),m.y)&&b.isInsidePlot(m.x+(b.inverted?c?-g.width:g.width:g.width/2),m.y+g.height))||m.hide())}},getStackBox:function(c,m,g,q,h,a,b){var d=m.axis.reversed,e=c.inverted;c=b.height+b.pos-(e?c.plotLeft:c.plotTop);m=m.isNegative&&!d||!m.isNegative&&d;return{x:e?m?q:q-a:g,y:e?c-g-h:m?c-q-a:c-q,width:e?a:h,height:e?h:a}}};w.prototype.getStacks=function(){var c=this,m=c.inverted;c.yAxis.forEach(function(c){c.stacks&&c.hasVisibleSeries&&
(c.oldStacks=c.stacks)});c.series.forEach(function(g){var p=g.xAxis&&g.xAxis.options||{};!g.options.stacking||!0!==g.visible&&!1!==c.options.chart.ignoreHiddenSeries||(g.stackKey=[g.type,y(g.options.stack,""),m?p.top:p.left,m?p.height:p.width].join())})};g.prototype.buildStacks=function(){var c=this.series,m=y(this.options.reversedStacks,!0),g=c.length,q;if(!this.isXAxis){this.usePercentage=!1;for(q=g;q--;)c[m?q:g-q-1].setStackedPoints();for(q=0;q<g;q++)c[q].modifyStacks()}};g.prototype.renderStackTotals=
function(){var c=this.chart,m=c.renderer,g=this.stacks,q=this.stackTotalGroup;q||(this.stackTotalGroup=q=m.g("stack-labels").attr({visibility:"visible",zIndex:6}).add());q.translate(c.plotLeft,c.plotTop);H(g,function(c){H(c,function(a){a.render(q)})})};g.prototype.resetStacks=function(){var c=this,m=c.stacks;c.isXAxis||H(m,function(m){H(m,function(g,h){g.touched<c.stacksTouched?(g.destroy(),delete m[h]):(g.total=null,g.cumulative=null)})})};g.prototype.cleanStacks=function(){if(!this.isXAxis){if(this.oldStacks)var c=
this.stacks=this.oldStacks;H(c,function(c){H(c,function(c){c.cumulative=c.total})})}};F.prototype.setStackedPoints=function(){if(this.options.stacking&&(!0===this.visible||!1===this.chart.options.chart.ignoreHiddenSeries)){var g=this.processedXData,m=this.processedYData,p=[],q=m.length,h=this.options,a=h.threshold,b=y(h.startFromThreshold&&a,0),d=h.stack;h=h.stacking;var e=this.stackKey,k="-"+e,C=this.negStacks,z=this.yAxis,r=z.stacks,n=z.oldStacks,f,l;z.stacksTouched+=1;for(l=0;l<q;l++){var v=g[l];
var B=m[l];var A=this.getStackIndicator(A,v,this.index);var u=A.key;var J=(f=C&&B<(b?0:a))?k:e;r[J]||(r[J]={});r[J][v]||(n[J]&&n[J][v]?(r[J][v]=n[J][v],r[J][v].total=null):r[J][v]=new c.StackItem(z,z.options.stackLabels,f,v,d));J=r[J][v];null!==B?(J.points[u]=J.points[this.index]=[y(J.cumulative,b)],I(J.cumulative)||(J.base=u),J.touched=z.stacksTouched,0<A.index&&!1===this.singleStacks&&(J.points[u][0]=J.points[this.index+","+v+",0"][0])):J.points[u]=J.points[this.index]=null;"percent"===h?(f=f?e:
k,C&&r[f]&&r[f][v]?(f=r[f][v],J.total=f.total=Math.max(f.total,J.total)+Math.abs(B)||0):J.total=x(J.total+(Math.abs(B)||0))):J.total=x(J.total+(B||0));J.cumulative=y(J.cumulative,b)+(B||0);null!==B&&(J.points[u].push(J.cumulative),p[l]=J.cumulative)}"percent"===h&&(z.usePercentage=!0);this.stackedYData=p;z.oldStacks={}}};F.prototype.modifyStacks=function(){var c=this,m=c.stackKey,g=c.yAxis.stacks,q=c.processedXData,h,a=c.options.stacking;c[a+"Stacker"]&&[m,"-"+m].forEach(function(b){for(var d=q.length,
e,k;d--;)if(e=q[d],h=c.getStackIndicator(h,e,c.index,b),k=(e=g[b]&&g[b][e])&&e.points[h.key])c[a+"Stacker"](k,e,d)})};F.prototype.percentStacker=function(c,m,g){m=m.total?100/m.total:0;c[0]=x(c[0]*m);c[1]=x(c[1]*m);this.stackedYData[g]=c[1]};F.prototype.getStackIndicator=function(c,m,g,q){!I(c)||c.x!==m||q&&c.key!==q?c={x:m,index:0,key:q}:c.index++;c.key=[g,m,c.index].join();return c}});K(D,"parts/Dynamics.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.erase,
H=g.extend,y=g.isArray,w=g.isNumber,x=g.isObject,E=g.isString,F=g.objectEach,t=g.pick,m=g.setAnimation,p=g.splat,q=c.addEvent,h=c.animate,a=c.Axis;g=c.Chart;var b=c.createElement,d=c.css,e=c.fireEvent,k=c.merge,C=c.Point,z=c.Series,r=c.seriesTypes;c.cleanRecursively=function(a,b){var f={};F(a,function(d,l){if(x(a[l],!0)&&!a.nodeType&&b[l])d=c.cleanRecursively(a[l],b[l]),Object.keys(d).length&&(f[l]=d);else if(x(a[l])||a[l]!==b[l])f[l]=a[l]});return f};H(g.prototype,{addSeries:function(a,b,c){var f,
d=this;a&&(b=t(b,!0),e(d,"addSeries",{options:a},function(){f=d.initSeries(a);d.isDirtyLegend=!0;d.linkSeries();e(d,"afterAddSeries",{series:f});b&&d.redraw(c)}));return f},addAxis:function(a,b,d,c){return this.createAxis(b?"xAxis":"yAxis",{axis:a,redraw:d,animation:c})},addColorAxis:function(a,b,d){return this.createAxis("colorAxis",{axis:a,redraw:b,animation:d})},createAxis:function(b,f){var d=this.options,e="colorAxis"===b,n=f.redraw,h=f.animation;f=k(f.axis,{index:this[b].length,isX:"xAxis"===
b});var m=e?new c.ColorAxis(this,f):new a(this,f);d[b]=p(d[b]||{});d[b].push(f);e&&(this.isDirtyLegend=!0,this.axes.forEach(function(a){a.series=[]}),this.series.forEach(function(a){a.bindAxes();a.isDirtyData=!0}));t(n,!0)&&this.redraw(h);return m},showLoading:function(a){var f=this,c=f.options,e=f.loadingDiv,k=c.loading,n=function(){e&&d(e,{left:f.plotLeft+"px",top:f.plotTop+"px",width:f.plotWidth+"px",height:f.plotHeight+"px"})};e||(f.loadingDiv=e=b("div",{className:"highcharts-loading highcharts-loading-hidden"},
null,f.container),f.loadingSpan=b("span",{className:"highcharts-loading-inner"},null,e),q(f,"redraw",n));e.className="highcharts-loading";f.loadingSpan.innerHTML=t(a,c.lang.loading,"");f.styledMode||(d(e,H(k.style,{zIndex:10})),d(f.loadingSpan,k.labelStyle),f.loadingShown||(d(e,{opacity:0,display:""}),h(e,{opacity:k.style.opacity||.5},{duration:k.showDuration||0})));f.loadingShown=!0;n()},hideLoading:function(){var a=this.options,b=this.loadingDiv;b&&(b.className="highcharts-loading highcharts-loading-hidden",
this.styledMode||h(b,{opacity:0},{duration:a.loading.hideDuration||100,complete:function(){d(b,{display:"none"})}}));this.loadingShown=!1},propsRequireDirtyBox:"backgroundColor borderColor borderWidth borderRadius plotBackgroundColor plotBackgroundImage plotBorderColor plotBorderWidth plotShadow shadow".split(" "),propsRequireReflow:"margin marginTop marginRight marginBottom marginLeft spacing spacingTop spacingRight spacingBottom spacingLeft".split(" "),propsRequireUpdateSeries:"chart.inverted chart.polar chart.ignoreHiddenSeries chart.type colors plotOptions time tooltip".split(" "),
collectionsWithUpdate:"xAxis yAxis zAxis colorAxis series pane".split(" "),update:function(a,b,d,h){var f=this,l={credits:"addCredits",title:"setTitle",subtitle:"setSubtitle",caption:"setCaption"},n,m,g,r=a.isResponsiveOptions,v=[];e(f,"update",{options:a});r||f.setResponsive(!1,!0);a=c.cleanRecursively(a,f.options);k(!0,f.userOptions,a);if(n=a.chart){k(!0,f.options.chart,n);"className"in n&&f.setClassName(n.className);"reflow"in n&&f.setReflow(n.reflow);if("inverted"in n||"polar"in n||"type"in n){f.propFromSeries();
var C=!0}"alignTicks"in n&&(C=!0);F(n,function(a,b){-1!==f.propsRequireUpdateSeries.indexOf("chart."+b)&&(m=!0);-1!==f.propsRequireDirtyBox.indexOf(b)&&(f.isDirtyBox=!0);r||-1===f.propsRequireReflow.indexOf(b)||(g=!0)});!f.styledMode&&"style"in n&&f.renderer.setStyle(n.style)}!f.styledMode&&a.colors&&(this.options.colors=a.colors);a.plotOptions&&k(!0,this.options.plotOptions,a.plotOptions);a.time&&this.time===c.time&&(this.time=new c.Time(a.time));F(a,function(a,b){if(f[b]&&"function"===typeof f[b].update)f[b].update(a,
!1);else if("function"===typeof f[l[b]])f[l[b]](a);"chart"!==b&&-1!==f.propsRequireUpdateSeries.indexOf(b)&&(m=!0)});this.collectionsWithUpdate.forEach(function(b){if(a[b]){if("series"===b){var c=[];f[b].forEach(function(a,b){a.options.isInternal||c.push(t(a.options.index,b))})}p(a[b]).forEach(function(a,l){(l=I(a.id)&&f.get(a.id)||f[b][c?c[l]:l])&&l.coll===b&&(l.update(a,!1),d&&(l.touched=!0));!l&&d&&f.collectionsWithInit[b]&&(f.collectionsWithInit[b][0].apply(f,[a].concat(f.collectionsWithInit[b][1]||
[]).concat([!1])).touched=!0)});d&&f[b].forEach(function(a){a.touched||a.options.isInternal?delete a.touched:v.push(a)})}});v.forEach(function(a){a.remove&&a.remove(!1)});C&&f.axes.forEach(function(a){a.update({},!1)});m&&f.series.forEach(function(a){a.update({},!1)});a.loading&&k(!0,f.options.loading,a.loading);C=n&&n.width;n=n&&n.height;E(n)&&(n=c.relativeLength(n,C||f.chartWidth));g||w(C)&&C!==f.chartWidth||w(n)&&n!==f.chartHeight?f.setSize(C,n,h):t(b,!0)&&f.redraw(h);e(f,"afterUpdate",{options:a,
redraw:b,animation:h})},setSubtitle:function(a,b){this.applyDescription("subtitle",a);this.layOutTitles(b)},setCaption:function(a,b){this.applyDescription("caption",a);this.layOutTitles(b)}});g.prototype.collectionsWithInit={xAxis:[g.prototype.addAxis,[!0]],yAxis:[g.prototype.addAxis,[!1]],colorAxis:[g.prototype.addColorAxis,[!1]],series:[g.prototype.addSeries]};H(C.prototype,{update:function(a,b,d,c){function f(){l.applyOptions(a);null===l.y&&k&&(l.graphic=k.destroy());x(a,!0)&&(k&&k.element&&a&&
a.marker&&void 0!==a.marker.symbol&&(l.graphic=k.destroy()),a&&a.dataLabels&&l.dataLabel&&(l.dataLabel=l.dataLabel.destroy()),l.connector&&(l.connector=l.connector.destroy()));n=l.index;e.updateParallelArrays(l,n);m.data[n]=x(m.data[n],!0)||x(a,!0)?l.options:t(a,m.data[n]);e.isDirty=e.isDirtyData=!0;!e.fixedBox&&e.hasCartesianSeries&&(h.isDirtyBox=!0);"point"===m.legendType&&(h.isDirtyLegend=!0);b&&h.redraw(d)}var l=this,e=l.series,k=l.graphic,n,h=e.chart,m=e.options;b=t(b,!0);!1===c?f():l.firePointEvent("update",
{options:a},f)},remove:function(a,b){this.series.removePoint(this.series.data.indexOf(this),a,b)}});H(z.prototype,{addPoint:function(a,b,d,c,k){var f=this.options,l=this.data,n=this.chart,h=this.xAxis;h=h&&h.hasNames&&h.names;var m=f.data,g=this.xData,r;b=t(b,!0);var B={series:this};this.pointClass.prototype.applyOptions.apply(B,[a]);var v=B.x;var p=g.length;if(this.requireSorting&&v<g[p-1])for(r=!0;p&&g[p-1]>v;)p--;this.updateParallelArrays(B,"splice",p,0,0);this.updateParallelArrays(B,p);h&&B.name&&
(h[v]=B.name);m.splice(p,0,a);r&&(this.data.splice(p,0,null),this.processData());"point"===f.legendType&&this.generatePoints();d&&(l[0]&&l[0].remove?l[0].remove(!1):(l.shift(),this.updateParallelArrays(B,"shift"),m.shift()));!1!==k&&e(this,"addPoint",{point:B});this.isDirtyData=this.isDirty=!0;b&&n.redraw(c)},removePoint:function(a,b,d){var f=this,c=f.data,l=c[a],e=f.points,k=f.chart,n=function(){e&&e.length===c.length&&e.splice(a,1);c.splice(a,1);f.options.data.splice(a,1);f.updateParallelArrays(l||
{series:f},"splice",a,1);l&&l.destroy();f.isDirty=!0;f.isDirtyData=!0;b&&k.redraw()};m(d,k);b=t(b,!0);l?l.firePointEvent("remove",null,n):n()},remove:function(a,b,d,c){function f(){l.destroy(c);l.remove=null;k.isDirtyLegend=k.isDirtyBox=!0;k.linkSeries();t(a,!0)&&k.redraw(b)}var l=this,k=l.chart;!1!==d?e(l,"remove",null,f):f()},update:function(a,b){a=c.cleanRecursively(a,this.userOptions);e(this,"update",{options:a});var f=this,d=f.chart,n=f.userOptions,h=f.initialType||f.type,m=a.type||n.type||d.options.chart.type,
g=!(this.hasDerivedData||a.dataGrouping||m&&m!==this.type||void 0!==a.pointStart||a.pointInterval||a.pointIntervalUnit||a.keys),p=r[h].prototype,C,q=["group","markerGroup","dataLabelsGroup","transformGroup"],z=["eventOptions","navigatorSeries","baseSeries"],x=f.finishedAnimating&&{animation:!1},w={};g&&(z.push("data","isDirtyData","points","processedXData","processedYData","xIncrement","_hasPointMarkers","_hasPointLabels","mapMap","mapData","minY","maxY","minX","maxX"),!1!==a.visible&&z.push("area",
"graph"),f.parallelArrays.forEach(function(a){z.push(a+"Data")}),a.data&&this.setData(a.data,!1));a=k(n,x,{index:void 0===n.index?f.index:n.index,pointStart:t(n.pointStart,f.xData[0])},!g&&{data:f.options.data},a);g&&a.data&&(a.data=f.options.data);z=q.concat(z);z.forEach(function(a){z[a]=f[a];delete f[a]});f.remove(!1,null,!1,!0);for(C in p)f[C]=void 0;r[m||h]?H(f,r[m||h].prototype):c.error(17,!0,d,{missingModuleFor:m||h});z.forEach(function(a){f[a]=z[a]});f.init(d,a);if(g&&this.points){var E=f.options;
!1===E.visible?(w.graphic=1,w.dataLabel=1):f._hasPointLabels||(m=E.marker,p=E.dataLabels,m&&(!1===m.enabled||"symbol"in m)&&(w.graphic=1),p&&!1===p.enabled&&(w.dataLabel=1));this.points.forEach(function(a){a&&a.series&&(a.resolveColor(),Object.keys(w).length&&a.destroyElements(w),!1===E.showInLegend&&a.legendItem&&d.legend.destroyItem(a))},this)}a.zIndex!==n.zIndex&&q.forEach(function(b){f[b]&&f[b].attr({zIndex:a.zIndex})});f.initialType=h;d.linkSeries();e(this,"afterUpdate");t(b,!0)&&d.redraw(g?
void 0:!1)},setName:function(a){this.name=this.options.name=this.userOptions.name=a;this.chart.isDirtyLegend=!0}});H(a.prototype,{update:function(a,b){var f=this.chart,d=a&&a.events||{};a=k(this.userOptions,a);f.options[this.coll].indexOf&&(f.options[this.coll][f.options[this.coll].indexOf(this.userOptions)]=a);F(f.options[this.coll].events,function(a,b){"undefined"===typeof d[b]&&(d[b]=void 0)});this.destroy(!0);this.init(f,H(a,{events:d}));f.isDirtyBox=!0;t(b,!0)&&f.redraw()},remove:function(a){for(var b=
this.chart,d=this.coll,c=this.series,e=c.length;e--;)c[e]&&c[e].remove(!1);G(b.axes,this);G(b[d],this);y(b.options[d])?b.options[d].splice(this.options.index,1):delete b.options[d];b[d].forEach(function(a,b){a.options.index=a.userOptions.index=b});this.destroy();b.isDirtyBox=!0;t(a,!0)&&b.redraw()},setTitle:function(a,b){this.update({title:a},b)},setCategories:function(a,b){this.update({categories:a},b)}})});K(D,"parts/AreaSeries.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=
g.objectEach,G=g.pick,H=c.color,y=c.Series;g=c.seriesType;g("area","line",{softThreshold:!1,threshold:0},{singleStacks:!1,getStackPoints:function(c){var g=[],w=[],y=this.xAxis,t=this.yAxis,m=t.stacks[this.stackKey],p={},q=this.index,h=t.series,a=h.length,b=G(t.options.reversedStacks,!0)?1:-1,d;c=c||this.points;if(this.options.stacking){for(d=0;d<c.length;d++)c[d].leftNull=c[d].rightNull=void 0,p[c[d].x]=c[d];I(m,function(a,b){null!==a.total&&w.push(b)});w.sort(function(a,b){return a-b});var e=h.map(function(a){return a.visible});
w.forEach(function(c,h){var k=0,r,n;if(p[c]&&!p[c].isNull)g.push(p[c]),[-1,1].forEach(function(f){var l=1===f?"rightNull":"leftNull",k=0,g=m[w[h+f]];if(g)for(d=q;0<=d&&d<a;)r=g.points[d],r||(d===q?p[c][l]=!0:e[d]&&(n=m[c].points[d])&&(k-=n[1]-n[0])),d+=b;p[c][1===f?"rightCliff":"leftCliff"]=k});else{for(d=q;0<=d&&d<a;){if(r=m[c].points[d]){k=r[1];break}d+=b}k=t.translate(k,0,1,0,1);g.push({isNull:!0,plotX:y.translate(c,0,0,0,1),x:c,plotY:k,yBottom:k})}})}return g},getGraphPath:function(c){var g=y.prototype.getGraphPath,
w=this.options,F=w.stacking,t=this.yAxis,m,p=[],q=[],h=this.index,a=t.stacks[this.stackKey],b=w.threshold,d=Math.round(t.getThreshold(w.threshold));w=G(w.connectNulls,"percent"===F);var e=function(e,k,f){var l=c[e];e=F&&a[l.x].points[h];var n=l[f+"Null"]||0;f=l[f+"Cliff"]||0;l=!0;if(f||n){var m=(n?e[0]:e[1])+f;var g=e[0]+f;l=!!n}else!F&&c[k]&&c[k].isNull&&(m=g=b);void 0!==m&&(q.push({plotX:C,plotY:null===m?d:t.getThreshold(m),isNull:l,isCliff:!0}),p.push({plotX:C,plotY:null===g?d:t.getThreshold(g),
doCurve:!1}))};c=c||this.points;F&&(c=this.getStackPoints(c));for(m=0;m<c.length;m++){F||(c[m].leftCliff=c[m].rightCliff=c[m].leftNull=c[m].rightNull=void 0);var k=c[m].isNull;var C=G(c[m].rectPlotX,c[m].plotX);var z=G(c[m].yBottom,d);if(!k||w)w||e(m,m-1,"left"),k&&!F&&w||(q.push(c[m]),p.push({x:m,plotX:C,plotY:z})),w||e(m,m+1,"right")}m=g.call(this,q,!0,!0);p.reversed=!0;k=g.call(this,p,!0,!0);k.length&&(k[0]="L");k=m.concat(k);g=g.call(this,q,!1,w);k.xMap=m.xMap;this.areaPath=k;return g},drawGraph:function(){this.areaPath=
[];y.prototype.drawGraph.apply(this);var c=this,g=this.areaPath,E=this.options,F=[["area","highcharts-area",this.color,E.fillColor]];this.zones.forEach(function(g,m){F.push(["zone-area-"+m,"highcharts-area highcharts-zone-area-"+m+" "+g.className,g.color||c.color,g.fillColor||E.fillColor])});F.forEach(function(t){var m=t[0],p=c[m],q=p?"animate":"attr",h={};p?(p.endX=c.preventGraphAnimation?null:g.xMap,p.animate({d:g})):(h.zIndex=0,p=c[m]=c.chart.renderer.path(g).addClass(t[1]).add(c.group),p.isArea=
!0);c.chart.styledMode||(h.fill=G(t[3],H(t[2]).setOpacity(G(E.fillOpacity,.75)).get()));p[q](h);p.startX=g.xMap;p.shiftUnit=E.step?2:1})},drawLegendSymbol:c.LegendSymbolMixin.drawRectangle});""});K(D,"parts/SplineSeries.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.pick;c=c.seriesType;c("spline","line",{},{getPointSpline:function(c,g,y){var w=g.plotX,x=g.plotY,E=c[y-1];y=c[y+1];if(E&&!E.isNull&&!1!==E.doCurve&&!g.isCliff&&y&&!y.isNull&&!1!==y.doCurve&&!g.isCliff){c=E.plotY;
var F=y.plotX;y=y.plotY;var t=0;var m=(1.5*w+E.plotX)/2.5;var p=(1.5*x+c)/2.5;F=(1.5*w+F)/2.5;var q=(1.5*x+y)/2.5;F!==m&&(t=(q-p)*(F-w)/(F-m)+x-q);p+=t;q+=t;p>c&&p>x?(p=Math.max(c,x),q=2*x-p):p<c&&p<x&&(p=Math.min(c,x),q=2*x-p);q>y&&q>x?(q=Math.max(y,x),p=2*x-q):q<y&&q<x&&(q=Math.min(y,x),p=2*x-q);g.rightContX=F;g.rightContY=q}g=["C",I(E.rightContX,E.plotX),I(E.rightContY,E.plotY),I(m,w),I(p,x),w,x];E.rightContX=E.rightContY=null;return g}});""});K(D,"parts/AreaSplineSeries.js",[D["parts/Globals.js"]],
function(c){var g=c.seriesTypes.area.prototype,I=c.seriesType;I("areaspline","spline",c.defaultPlotOptions.area,{getStackPoints:g.getStackPoints,getGraphPath:g.getGraphPath,drawGraph:g.drawGraph,drawLegendSymbol:c.LegendSymbolMixin.drawRectangle});""});K(D,"parts/ColumnSeries.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.extend,H=g.isNumber,y=g.pick,w=c.animObject,x=c.color,E=c.merge,F=c.Series;g=c.seriesType;var t=c.svg;g("column","line",{borderRadius:0,crisp:!0,
groupPadding:.2,marker:null,pointPadding:.1,minPointLength:0,cropThreshold:50,pointRange:null,states:{hover:{halo:!1,brightness:.1},select:{color:"#cccccc",borderColor:"#000000"}},dataLabels:{align:null,verticalAlign:null,y:null},softThreshold:!1,startFromThreshold:!0,stickyTracking:!1,tooltip:{distance:6},threshold:0,borderColor:"#ffffff"},{cropShoulder:0,directTouch:!0,trackerGroups:["group","dataLabelsGroup"],negStacks:!0,init:function(){F.prototype.init.apply(this,arguments);var c=this,g=c.chart;
g.hasRendered&&g.series.forEach(function(m){m.type===c.type&&(m.isDirty=!0)})},getColumnMetrics:function(){var c=this,g=c.options,q=c.xAxis,h=c.yAxis,a=q.options.reversedStacks;a=q.reversed&&!a||!q.reversed&&a;var b,d={},e=0;!1===g.grouping?e=1:c.chart.series.forEach(function(a){var k=a.yAxis,f=a.options;if(a.type===c.type&&(a.visible||!c.chart.options.chart.ignoreHiddenSeries)&&h.len===k.len&&h.pos===k.pos){if(f.stacking){b=a.stackKey;void 0===d[b]&&(d[b]=e++);var l=d[b]}else!1!==f.grouping&&(l=
e++);a.columnIndex=l}});var k=Math.min(Math.abs(q.transA)*(q.ordinalSlope||g.pointRange||q.closestPointRange||q.tickInterval||1),q.len),C=k*g.groupPadding,z=(k-2*C)/(e||1);g=Math.min(g.maxPointWidth||q.len,y(g.pointWidth,z*(1-2*g.pointPadding)));c.columnMetrics={width:g,offset:(z-g)/2+(C+((c.columnIndex||0)+(a?1:0))*z-k/2)*(a?-1:1)};return c.columnMetrics},crispCol:function(c,g,q,h){var a=this.chart,b=this.borderWidth,d=-(b%2?.5:0);b=b%2?.5:1;a.inverted&&a.renderer.isVML&&(b+=1);this.options.crisp&&
(q=Math.round(c+q)+d,c=Math.round(c)+d,q-=c);h=Math.round(g+h)+b;d=.5>=Math.abs(g)&&.5<h;g=Math.round(g)+b;h-=g;d&&h&&(--g,h+=1);return{x:c,y:g,width:q,height:h}},translate:function(){var c=this,g=c.chart,q=c.options,h=c.dense=2>c.closestPointRange*c.xAxis.transA;h=c.borderWidth=y(q.borderWidth,h?0:1);var a=c.yAxis,b=q.threshold,d=c.translatedThreshold=a.getThreshold(b),e=y(q.minPointLength,5),k=c.getColumnMetrics(),C=k.width,z=c.barW=Math.max(C,1+2*h),r=c.pointXOffset=k.offset,n=c.dataMin,f=c.dataMax;
g.inverted&&(d-=.5);q.pointPadding&&(z=Math.ceil(z));F.prototype.translate.apply(c);c.points.forEach(function(l){var k=y(l.yBottom,d),h=999+Math.abs(k),m=C;h=Math.min(Math.max(-h,l.plotY),a.len+h);var u=l.plotX+r,p=z,q=Math.min(h,k),t=Math.max(h,k)-q;if(e&&Math.abs(t)<e){t=e;var x=!a.reversed&&!l.negative||a.reversed&&l.negative;l.y===b&&c.dataMax<=b&&a.min<b&&n!==f&&(x=!x);q=Math.abs(q-d)>e?k-e:d-(x?e:0)}I(l.options.pointWidth)&&(m=p=Math.ceil(l.options.pointWidth),u-=Math.round((m-C)/2));l.barX=
u;l.pointWidth=m;l.tooltipPos=g.inverted?[a.len+a.pos-g.plotLeft-h,c.xAxis.len-u-p/2,t]:[u+p/2,h+a.pos-g.plotTop,t];l.shapeType=c.pointClass.prototype.shapeType||"rect";l.shapeArgs=c.crispCol.apply(c,l.isNull?[u,d,p,0]:[u,q,p,t])})},getSymbol:c.noop,drawLegendSymbol:c.LegendSymbolMixin.drawRectangle,drawGraph:function(){this.group[this.dense?"addClass":"removeClass"]("highcharts-dense-data")},pointAttribs:function(c,g){var m=this.options,h=this.pointAttrToOptions||{};var a=h.stroke||"borderColor";
var b=h["stroke-width"]||"borderWidth",d=c&&c.color||this.color,e=c&&c[a]||m[a]||this.color||d,k=c&&c[b]||m[b]||this[b]||0;h=c&&c.options.dashStyle||m.dashStyle;var p=y(m.opacity,1);if(c&&this.zones.length){var z=c.getZone();d=c.options.color||z&&(z.color||c.nonZonedColor)||this.color;z&&(e=z.borderColor||e,h=z.dashStyle||h,k=z.borderWidth||k)}g&&(c=E(m.states[g],c.options.states&&c.options.states[g]||{}),g=c.brightness,d=c.color||void 0!==g&&x(d).brighten(c.brightness).get()||d,e=c[a]||e,k=c[b]||
k,h=c.dashStyle||h,p=y(c.opacity,p));a={fill:d,stroke:e,"stroke-width":k,opacity:p};h&&(a.dashstyle=h);return a},drawPoints:function(){var c=this,g=this.chart,q=c.options,h=g.renderer,a=q.animationLimit||250,b;c.points.forEach(function(d){var e=d.graphic,k=e&&g.pointCount<a?"animate":"attr";if(H(d.plotY)&&null!==d.y){b=d.shapeArgs;e&&d.hasNewShapeType()&&(e=e.destroy());if(e)e[k](E(b));else d.graphic=e=h[d.shapeType](b).add(d.group||c.group);if(q.borderRadius)e[k]({r:q.borderRadius});g.styledMode||
e[k](c.pointAttribs(d,d.selected&&"select")).shadow(!1!==d.allowShadow&&q.shadow,null,q.stacking&&!q.borderRadius);e.addClass(d.getClassName(),!0)}else e&&(d.graphic=e.destroy())})},animate:function(c){var g=this,m=this.yAxis,h=g.options,a=this.chart.inverted,b={},d=a?"translateX":"translateY";if(t)if(c)b.scaleY=.001,c=Math.min(m.pos+m.len,Math.max(m.pos,m.toPixels(h.threshold))),a?b.translateX=c-m.len:b.translateY=c,g.clipBox&&g.setClip(),g.group.attr(b);else{var e=g.group.attr(d);g.group.animate({scaleY:1},
G(w(g.options.animation),{step:function(a,c){b[d]=e+c.pos*(m.pos-e);g.group.attr(b)}}));g.animate=null}},remove:function(){var c=this,g=c.chart;g.hasRendered&&g.series.forEach(function(g){g.type===c.type&&(g.isDirty=!0)});F.prototype.remove.apply(c,arguments)}});""});K(D,"parts/BarSeries.js",[D["parts/Globals.js"]],function(c){c=c.seriesType;c("bar","column",null,{inverted:!0});""});K(D,"parts/ScatterSeries.js",[D["parts/Globals.js"]],function(c){var g=c.Series,I=c.seriesType;I("scatter","line",{lineWidth:0,
findNearestPointBy:"xy",jitter:{x:0,y:0},marker:{enabled:!0},tooltip:{headerFormat:'<span style="color:{point.color}">\u25cf</span> <span style="font-size: 10px"> {series.name}</span><br/>',pointFormat:"x: <b>{point.x}</b><br/>y: <b>{point.y}</b><br/>"}},{sorted:!1,requireSorting:!1,noSharedTooltip:!0,trackerGroups:["group","markerGroup","dataLabelsGroup"],takeOrdinalPosition:!1,drawGraph:function(){this.options.lineWidth&&g.prototype.drawGraph.call(this)},applyJitter:function(){var c=this,g=this.options.jitter,
y=this.points.length;g&&this.points.forEach(function(w,x){["x","y"].forEach(function(E,F){var t="plot"+E.toUpperCase();if(g[E]&&!w.isNull){var m=c[E+"Axis"];var p=g[E]*m.transA;if(m&&!m.isLog){var q=Math.max(0,w[t]-p);m=Math.min(m.len,w[t]+p);F=1E4*Math.sin(x+F*y);w[t]=q+(m-q)*(F-Math.floor(F));"x"===E&&(w.clientX=w.plotX)}}})})}});c.addEvent(g,"afterTranslate",function(){this.applyJitter&&this.applyJitter()});""});K(D,"mixins/centered-series.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,
g){var I=g.isNumber,G=g.pick,H=c.deg2rad,y=c.relativeLength;c.CenteredSeriesMixin={getCenter:function(){var c=this.options,g=this.chart,E=2*(c.slicedOffset||0),F=g.plotWidth-2*E;g=g.plotHeight-2*E;var t=c.center;t=[G(t[0],"50%"),G(t[1],"50%"),c.size||"100%",c.innerSize||0];var m=Math.min(F,g),p;for(p=0;4>p;++p){var q=t[p];c=2>p||2===p&&/%$/.test(q);t[p]=y(q,[F,g,m,t[2]][p])+(c?E:0)}t[3]>t[2]&&(t[3]=t[2]);return t},getStartAndEndRadians:function(c,g){c=I(c)?c:0;g=I(g)&&g>c&&360>g-c?g:c+360;return{start:H*
(c+-90),end:H*(g+-90)}}}});K(D,"parts/PieSeries.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.defined,G=g.isNumber,H=g.pick,y=g.setAnimation,w=c.addEvent;g=c.CenteredSeriesMixin;var x=g.getStartAndEndRadians,E=c.merge,F=c.noop,t=c.Point,m=c.Series,p=c.seriesType,q=c.fireEvent;p("pie","line",{center:[null,null],clip:!1,colorByPoint:!0,dataLabels:{allowOverlap:!0,connectorPadding:5,distance:30,enabled:!0,formatter:function(){return this.point.isNull?void 0:this.point.name},
softConnector:!0,x:0,connectorShape:"fixedOffset",crookDistance:"70%"},fillColor:void 0,ignoreHiddenPoint:!0,inactiveOtherPoints:!0,legendType:"point",marker:null,size:null,showInLegend:!1,slicedOffset:10,stickyTracking:!1,tooltip:{followPointer:!0},borderColor:"#ffffff",borderWidth:1,lineWidth:void 0,states:{hover:{brightness:.1}}},{isCartesian:!1,requireSorting:!1,directTouch:!0,noSharedTooltip:!0,trackerGroups:["group","dataLabelsGroup"],axisTypes:[],pointAttribs:c.seriesTypes.column.prototype.pointAttribs,
animate:function(c){var a=this,b=a.points,d=a.startAngleRad;c||(b.forEach(function(b){var c=b.graphic,e=b.shapeArgs;c&&(c.attr({r:b.startR||a.center[3]/2,start:d,end:d}),c.animate({r:e.r,start:e.start,end:e.end},a.options.animation))}),a.animate=null)},hasData:function(){return!!this.processedXData.length},updateTotals:function(){var c,a=0,b=this.points,d=b.length,e=this.options.ignoreHiddenPoint;for(c=0;c<d;c++){var k=b[c];a+=e&&!k.visible?0:k.isNull?0:k.y}this.total=a;for(c=0;c<d;c++)k=b[c],k.percentage=
0<a&&(k.visible||!e)?k.y/a*100:0,k.total=a},generatePoints:function(){m.prototype.generatePoints.call(this);this.updateTotals()},getX:function(c,a,b){var d=this.center,e=this.radii?this.radii[b.index]:d[2]/2;return d[0]+(a?-1:1)*Math.cos(Math.asin(Math.max(Math.min((c-d[1])/(e+b.labelDistance),1),-1)))*(e+b.labelDistance)+(0<b.labelDistance?(a?-1:1)*this.options.dataLabels.padding:0)},translate:function(h){this.generatePoints();var a=0,b=this.options,d=b.slicedOffset,e=d+(b.borderWidth||0),k=x(b.startAngle,
b.endAngle),g=this.startAngleRad=k.start;k=(this.endAngleRad=k.end)-g;var m=this.points,r=b.dataLabels.distance;b=b.ignoreHiddenPoint;var n,f=m.length;h||(this.center=h=this.getCenter());for(n=0;n<f;n++){var l=m[n];var v=g+a*k;if(!b||l.visible)a+=l.percentage/100;var B=g+a*k;l.shapeType="arc";l.shapeArgs={x:h[0],y:h[1],r:h[2]/2,innerR:h[3]/2,start:Math.round(1E3*v)/1E3,end:Math.round(1E3*B)/1E3};l.labelDistance=H(l.options.dataLabels&&l.options.dataLabels.distance,r);l.labelDistance=c.relativeLength(l.labelDistance,
l.shapeArgs.r);this.maxLabelDistance=Math.max(this.maxLabelDistance||0,l.labelDistance);B=(B+v)/2;B>1.5*Math.PI?B-=2*Math.PI:B<-Math.PI/2&&(B+=2*Math.PI);l.slicedTranslation={translateX:Math.round(Math.cos(B)*d),translateY:Math.round(Math.sin(B)*d)};var A=Math.cos(B)*h[2]/2;var u=Math.sin(B)*h[2]/2;l.tooltipPos=[h[0]+.7*A,h[1]+.7*u];l.half=B<-Math.PI/2||B>Math.PI/2?1:0;l.angle=B;v=Math.min(e,l.labelDistance/5);l.labelPosition={natural:{x:h[0]+A+Math.cos(B)*l.labelDistance,y:h[1]+u+Math.sin(B)*l.labelDistance},
"final":{},alignment:0>l.labelDistance?"center":l.half?"right":"left",connectorPosition:{breakAt:{x:h[0]+A+Math.cos(B)*v,y:h[1]+u+Math.sin(B)*v},touchingSliceAt:{x:h[0]+A,y:h[1]+u}}}}q(this,"afterTranslate")},drawEmpty:function(){var c=this.options;if(0===this.total){var a=this.center[0];var b=this.center[1];this.graph||(this.graph=this.chart.renderer.circle(a,b,0).addClass("highcharts-graph").add(this.group));this.graph.animate({"stroke-width":c.borderWidth,cx:a,cy:b,r:this.center[2]/2,fill:c.fillColor||
"none",stroke:c.color||"#cccccc"})}else this.graph&&(this.graph=this.graph.destroy())},redrawPoints:function(){var c=this,a=c.chart,b=a.renderer,d,e,k,g,m=c.options.shadow;this.drawEmpty();!m||c.shadowGroup||a.styledMode||(c.shadowGroup=b.g("shadow").attr({zIndex:-1}).add(c.group));c.points.forEach(function(h){var n={};e=h.graphic;if(!h.isNull&&e){g=h.shapeArgs;d=h.getTranslate();if(!a.styledMode){var f=h.shadowGroup;m&&!f&&(f=h.shadowGroup=b.g("shadow").add(c.shadowGroup));f&&f.attr(d);k=c.pointAttribs(h,
h.selected&&"select")}h.delayedRendering?(e.setRadialReference(c.center).attr(g).attr(d),a.styledMode||e.attr(k).attr({"stroke-linejoin":"round"}).shadow(m,f),h.delayedRendering=!1):(e.setRadialReference(c.center),a.styledMode||E(!0,n,k),E(!0,n,g,d),e.animate(n));e.attr({visibility:h.visible?"inherit":"hidden"});e.addClass(h.getClassName())}else e&&(h.graphic=e.destroy())})},drawPoints:function(){var c=this.chart.renderer;this.points.forEach(function(a){a.graphic||(a.graphic=c[a.shapeType](a.shapeArgs).add(a.series.group),
a.delayedRendering=!0)})},searchPoint:F,sortByAngle:function(c,a){c.sort(function(b,c){return void 0!==b.angle&&(c.angle-b.angle)*a})},drawLegendSymbol:c.LegendSymbolMixin.drawRectangle,getCenter:g.getCenter,getSymbol:F,drawGraph:null},{init:function(){t.prototype.init.apply(this,arguments);var c=this;c.name=H(c.name,"Slice");var a=function(a){c.slice("select"===a.type)};w(c,"select",a);w(c,"unselect",a);return c},isValid:function(){return G(this.y)&&0<=this.y},setVisible:function(c,a){var b=this,
d=b.series,e=d.chart,k=d.options.ignoreHiddenPoint;a=H(a,k);c!==b.visible&&(b.visible=b.options.visible=c=void 0===c?!b.visible:c,d.options.data[d.data.indexOf(b)]=b.options,["graphic","dataLabel","connector","shadowGroup"].forEach(function(a){if(b[a])b[a][c?"show":"hide"](!0)}),b.legendItem&&e.legend.colorizeItem(b,c),c||"hover"!==b.state||b.setState(""),k&&(d.isDirty=!0),a&&e.redraw())},slice:function(c,a,b){var d=this.series;y(b,d.chart);H(a,!0);this.sliced=this.options.sliced=I(c)?c:!this.sliced;
d.options.data[d.data.indexOf(this)]=this.options;this.graphic.animate(this.getTranslate());this.shadowGroup&&this.shadowGroup.animate(this.getTranslate())},getTranslate:function(){return this.sliced?this.slicedTranslation:{translateX:0,translateY:0}},haloPath:function(c){var a=this.shapeArgs;return this.sliced||!this.visible?[]:this.series.chart.renderer.symbols.arc(a.x,a.y,a.r+c,a.r+c,{innerR:a.r-1,start:a.start,end:a.end})},connectorShapes:{fixedOffset:function(c,a,b){var d=a.breakAt;a=a.touchingSliceAt;
return["M",c.x,c.y].concat(b.softConnector?["C",c.x+("left"===c.alignment?-5:5),c.y,2*d.x-a.x,2*d.y-a.y,d.x,d.y]:["L",d.x,d.y]).concat(["L",a.x,a.y])},straight:function(c,a){a=a.touchingSliceAt;return["M",c.x,c.y,"L",a.x,a.y]},crookedLine:function(g,a,b){a=a.touchingSliceAt;var d=this.series,e=d.center[0],k=d.chart.plotWidth,h=d.chart.plotLeft;d=g.alignment;var m=this.shapeArgs.r;b=c.relativeLength(b.crookDistance,1);b="left"===d?e+m+(k+h-e-m)*(1-b):h+(e-m)*b;e=["L",b,g.y];if("left"===d?b>g.x||b<
a.x:b<g.x||b>a.x)e=[];return["M",g.x,g.y].concat(e).concat(["L",a.x,a.y])}},getConnectorPath:function(){var c=this.labelPosition,a=this.series.options.dataLabels,b=a.connectorShape,d=this.connectorShapes;d[b]&&(b=d[b]);return b.call(this,{x:c.final.x,y:c.final.y,alignment:c.alignment},c.connectorPosition,a)}});""});K(D,"parts/DataLabels.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var I=g.arrayMax,G=g.defined,H=g.extend,y=g.isArray,w=g.objectEach,x=g.pick,E=g.splat,F=c.format,
t=c.merge;g=c.noop;var m=c.relativeLength,p=c.Series,q=c.seriesTypes,h=c.stableSort;c.distribute=function(a,b,d){function e(a,b){return a.target-b.target}var k,g=!0,m=a,r=[];var n=0;var f=m.reducedLen||b;for(k=a.length;k--;)n+=a[k].size;if(n>f){h(a,function(a,b){return(b.rank||0)-(a.rank||0)});for(n=k=0;n<=f;)n+=a[k].size,k++;r=a.splice(k-1,a.length)}h(a,e);for(a=a.map(function(a){return{size:a.size,targets:[a.target],align:x(a.align,.5)}});g;){for(k=a.length;k--;)g=a[k],n=(Math.min.apply(0,g.targets)+
Math.max.apply(0,g.targets))/2,g.pos=Math.min(Math.max(0,n-g.size*g.align),b-g.size);k=a.length;for(g=!1;k--;)0<k&&a[k-1].pos+a[k-1].size>a[k].pos&&(a[k-1].size+=a[k].size,a[k-1].targets=a[k-1].targets.concat(a[k].targets),a[k-1].align=.5,a[k-1].pos+a[k-1].size>b&&(a[k-1].pos=b-a[k-1].size),a.splice(k,1),g=!0)}m.push.apply(m,r);k=0;a.some(function(a){var f=0;if(a.targets.some(function(){m[k].pos=a.pos+f;if(Math.abs(m[k].pos-m[k].target)>d)return m.slice(0,k+1).forEach(function(a){delete a.pos}),m.reducedLen=
(m.reducedLen||b)-.1*b,m.reducedLen>.1*b&&c.distribute(m,b,d),!0;f+=m[k].size;k++}))return!0});h(m,e)};p.prototype.drawDataLabels=function(){function a(a,b){var c=b.filter;return c?(b=c.operator,a=a[c.property],c=c.value,">"===b&&a>c||"<"===b&&a<c||">="===b&&a>=c||"<="===b&&a<=c||"=="===b&&a==c||"==="===b&&a===c?!0:!1):!0}function b(a,b){var c=[],f;if(y(a)&&!y(b))c=a.map(function(a){return t(a,b)});else if(y(b)&&!y(a))c=b.map(function(b){return t(a,b)});else if(y(a)||y(b))for(f=Math.max(a.length,
b.length);f--;)c[f]=t(a[f],b[f]);else c=t(a,b);return c}var d=this,e=d.chart,k=d.options,g=k.dataLabels,h=d.points,m,n=d.hasRendered||0,f=c.animObject(k.animation).duration,l=Math.min(f,200),v=!e.renderer.forExport&&x(g.defer,0<l),B=e.renderer;g=b(b(e.options.plotOptions&&e.options.plotOptions.series&&e.options.plotOptions.series.dataLabels,e.options.plotOptions&&e.options.plotOptions[d.type]&&e.options.plotOptions[d.type].dataLabels),g);c.fireEvent(this,"drawDataLabels");if(y(g)||g.enabled||d._hasPointLabels){var A=
d.plotGroup("dataLabelsGroup","data-labels",v&&!n?"hidden":"inherit",g.zIndex||6);v&&(A.attr({opacity:+n}),n||setTimeout(function(){var a=d.dataLabelsGroup;a&&(d.visible&&A.show(!0),a[k.animation?"animate":"attr"]({opacity:1},{duration:l}))},f-l));h.forEach(function(c){m=E(b(g,c.dlOptions||c.options&&c.options.dataLabels));m.forEach(function(b,f){var l=b.enabled&&(!c.isNull||c.dataLabelOnNull)&&a(c,b),n=c.dataLabels?c.dataLabels[f]:c.dataLabel,g=c.connectors?c.connectors[f]:c.connector,h=x(b.distance,
c.labelDistance),m=!n;if(l){var r=c.getLabelConfig();var v=x(b[c.formatPrefix+"Format"],b.format);r=G(v)?F(v,r,e.time):(b[c.formatPrefix+"Formatter"]||b.formatter).call(r,b);v=b.style;var u=b.rotation;e.styledMode||(v.color=x(b.color,v.color,d.color,"#000000"),"contrast"===v.color&&(c.contrastColor=B.getContrast(c.color||d.color),v.color=!G(h)&&b.inside||0>h||k.stacking?c.contrastColor:"#000000"),k.cursor&&(v.cursor=k.cursor));var p={r:b.borderRadius||0,rotation:u,padding:b.padding,zIndex:1};e.styledMode||
(p.fill=b.backgroundColor,p.stroke=b.borderColor,p["stroke-width"]=b.borderWidth);w(p,function(a,b){void 0===a&&delete p[b]})}!n||l&&G(r)?l&&G(r)&&(n?p.text=r:(c.dataLabels=c.dataLabels||[],n=c.dataLabels[f]=u?B.text(r,0,-9999).addClass("highcharts-data-label"):B.label(r,0,-9999,b.shape,null,null,b.useHTML,null,"data-label"),f||(c.dataLabel=n),n.addClass(" highcharts-data-label-color-"+c.colorIndex+" "+(b.className||"")+(b.useHTML?" highcharts-tracker":""))),n.options=b,n.attr(p),e.styledMode||n.css(v).shadow(b.shadow),
n.added||n.add(A),b.textPath&&!b.useHTML&&n.setTextPath(c.getDataLabelPath&&c.getDataLabelPath(n)||c.graphic,b.textPath),d.alignDataLabel(c,n,b,null,m)):(c.dataLabel=c.dataLabel&&c.dataLabel.destroy(),c.dataLabels&&(1===c.dataLabels.length?delete c.dataLabels:delete c.dataLabels[f]),f||delete c.dataLabel,g&&(c.connector=c.connector.destroy(),c.connectors&&(1===c.connectors.length?delete c.connectors:delete c.connectors[f])))})})}c.fireEvent(this,"afterDrawDataLabels")};p.prototype.alignDataLabel=
function(a,b,c,e,k){var d=this.chart,g=this.isCartesian&&d.inverted,h=x(a.dlBox&&a.dlBox.centerX,a.plotX,-9999),n=x(a.plotY,-9999),f=b.getBBox(),l=c.rotation,m=c.align,B=this.visible&&(a.series.forceDL||d.isInsidePlot(h,Math.round(n),g)||e&&d.isInsidePlot(h,g?e.x+1:e.y+e.height-1,g)),A="justify"===x(c.overflow,"justify");if(B){var u=d.renderer.fontMetrics(d.styledMode?void 0:c.style.fontSize,b).b;e=H({x:g?this.yAxis.len-n:h,y:Math.round(g?this.xAxis.len-h:n),width:0,height:0},e);H(c,{width:f.width,
height:f.height});l?(A=!1,h=d.renderer.rotCorr(u,l),h={x:e.x+c.x+e.width/2+h.x,y:e.y+c.y+{top:0,middle:.5,bottom:1}[c.verticalAlign]*e.height},b[k?"attr":"animate"](h).attr({align:m}),n=(l+720)%360,n=180<n&&360>n,"left"===m?h.y-=n?f.height:0:"center"===m?(h.x-=f.width/2,h.y-=f.height/2):"right"===m&&(h.x-=f.width,h.y-=n?0:f.height),b.placed=!0,b.alignAttr=h):(b.align(c,null,e),h=b.alignAttr);A&&0<=e.height?this.justifyDataLabel(b,c,h,f,e,k):x(c.crop,!0)&&(B=d.isInsidePlot(h.x,h.y)&&d.isInsidePlot(h.x+
f.width,h.y+f.height));if(c.shape&&!l)b[k?"attr":"animate"]({anchorX:g?d.plotWidth-a.plotY:a.plotX,anchorY:g?d.plotHeight-a.plotX:a.plotY})}B||(b.hide(!0),b.placed=!1)};p.prototype.justifyDataLabel=function(a,b,c,e,k,g){var d=this.chart,h=b.align,n=b.verticalAlign,f=a.box?0:a.padding||0;var l=c.x+f;if(0>l){"right"===h?(b.align="left",b.inside=!0):b.x=-l;var m=!0}l=c.x+e.width-f;l>d.plotWidth&&("left"===h?(b.align="right",b.inside=!0):b.x=d.plotWidth-l,m=!0);l=c.y+f;0>l&&("bottom"===n?(b.verticalAlign=
"top",b.inside=!0):b.y=-l,m=!0);l=c.y+e.height-f;l>d.plotHeight&&("top"===n?(b.verticalAlign="bottom",b.inside=!0):b.y=d.plotHeight-l,m=!0);m&&(a.placed=!g,a.align(b,null,k));return m};q.pie&&(q.pie.prototype.dataLabelPositioners={radialDistributionY:function(a){return a.top+a.distributeBox.pos},radialDistributionX:function(a,b,c,e){return a.getX(c<b.top+2||c>b.bottom-2?e:c,b.half,b)},justify:function(a,b,c){return c[0]+(a.half?-1:1)*(b+a.labelDistance)},alignToPlotEdges:function(a,b,c,e){a=a.getBBox().width;
return b?a+e:c-a-e},alignToConnectors:function(a,b,c,e){var d=0,g;a.forEach(function(a){g=a.dataLabel.getBBox().width;g>d&&(d=g)});return b?d+e:c-d-e}},q.pie.prototype.drawDataLabels=function(){var a=this,b=a.data,d,e=a.chart,k=a.options.dataLabels,g=k.connectorPadding,h,m=e.plotWidth,n=e.plotHeight,f=e.plotLeft,l=Math.round(e.chartWidth/3),v,B=a.center,A=B[2]/2,u=B[1],q,w,E,y,F=[[],[]],H,D,O,K,S=[0,0,0,0],ca=a.dataLabelPositioners,X;a.visible&&(k.enabled||a._hasPointLabels)&&(b.forEach(function(a){a.dataLabel&&
a.visible&&a.dataLabel.shortened&&(a.dataLabel.attr({width:"auto"}).css({width:"auto",textOverflow:"clip"}),a.dataLabel.shortened=!1)}),p.prototype.drawDataLabels.apply(a),b.forEach(function(a){a.dataLabel&&(a.visible?(F[a.half].push(a),a.dataLabel._pos=null,!G(k.style.width)&&!G(a.options.dataLabels&&a.options.dataLabels.style&&a.options.dataLabels.style.width)&&a.dataLabel.getBBox().width>l&&(a.dataLabel.css({width:.7*l}),a.dataLabel.shortened=!0)):(a.dataLabel=a.dataLabel.destroy(),a.dataLabels&&
1===a.dataLabels.length&&delete a.dataLabels))}),F.forEach(function(b,l){var h=b.length,r=[],v;if(h){a.sortByAngle(b,l-.5);if(0<a.maxLabelDistance){var p=Math.max(0,u-A-a.maxLabelDistance);var t=Math.min(u+A+a.maxLabelDistance,e.plotHeight);b.forEach(function(a){0<a.labelDistance&&a.dataLabel&&(a.top=Math.max(0,u-A-a.labelDistance),a.bottom=Math.min(u+A+a.labelDistance,e.plotHeight),v=a.dataLabel.getBBox().height||21,a.distributeBox={target:a.labelPosition.natural.y-a.top+v/2,size:v,rank:a.y},r.push(a.distributeBox))});
p=t+v-p;c.distribute(r,p,p/5)}for(K=0;K<h;K++){d=b[K];E=d.labelPosition;q=d.dataLabel;O=!1===d.visible?"hidden":"inherit";D=p=E.natural.y;r&&G(d.distributeBox)&&(void 0===d.distributeBox.pos?O="hidden":(y=d.distributeBox.size,D=ca.radialDistributionY(d)));delete d.positionIndex;if(k.justify)H=ca.justify(d,A,B);else switch(k.alignTo){case "connectors":H=ca.alignToConnectors(b,l,m,f);break;case "plotEdges":H=ca.alignToPlotEdges(q,l,m,f);break;default:H=ca.radialDistributionX(a,d,D,p)}q._attr={visibility:O,
align:E.alignment};q._pos={x:H+k.x+({left:g,right:-g}[E.alignment]||0),y:D+k.y-10};E.final.x=H;E.final.y=D;x(k.crop,!0)&&(w=q.getBBox().width,p=null,H-w<g&&1===l?(p=Math.round(w-H+g),S[3]=Math.max(p,S[3])):H+w>m-g&&0===l&&(p=Math.round(H+w-m+g),S[1]=Math.max(p,S[1])),0>D-y/2?S[0]=Math.max(Math.round(-D+y/2),S[0]):D+y/2>n&&(S[2]=Math.max(Math.round(D+y/2-n),S[2])),q.sideOverflow=p)}}}),0===I(S)||this.verifyDataLabelOverflow(S))&&(this.placeDataLabels(),this.points.forEach(function(b){X=t(k,b.options.dataLabels);
if(h=x(X.connectorWidth,1)){var c;v=b.connector;if((q=b.dataLabel)&&q._pos&&b.visible&&0<b.labelDistance){O=q._attr.visibility;if(c=!v)b.connector=v=e.renderer.path().addClass("highcharts-data-label-connector  highcharts-color-"+b.colorIndex+(b.className?" "+b.className:"")).add(a.dataLabelsGroup),e.styledMode||v.attr({"stroke-width":h,stroke:X.connectorColor||b.color||"#666666"});v[c?"attr":"animate"]({d:b.getConnectorPath()});v.attr("visibility",O)}else v&&(b.connector=v.destroy())}}))},q.pie.prototype.placeDataLabels=
function(){this.points.forEach(function(a){var b=a.dataLabel,c;b&&a.visible&&((c=b._pos)?(b.sideOverflow&&(b._attr.width=Math.max(b.getBBox().width-b.sideOverflow,0),b.css({width:b._attr.width+"px",textOverflow:(this.options.dataLabels.style||{}).textOverflow||"ellipsis"}),b.shortened=!0),b.attr(b._attr),b[b.moved?"animate":"attr"](c),b.moved=!0):b&&b.attr({y:-9999}));delete a.distributeBox},this)},q.pie.prototype.alignDataLabel=g,q.pie.prototype.verifyDataLabelOverflow=function(a){var b=this.center,
c=this.options,e=c.center,k=c.minSize||80,g=null!==c.size;if(!g){if(null!==e[0])var h=Math.max(b[2]-Math.max(a[1],a[3]),k);else h=Math.max(b[2]-a[1]-a[3],k),b[0]+=(a[3]-a[1])/2;null!==e[1]?h=Math.max(Math.min(h,b[2]-Math.max(a[0],a[2])),k):(h=Math.max(Math.min(h,b[2]-a[0]-a[2]),k),b[1]+=(a[0]-a[2])/2);h<b[2]?(b[2]=h,b[3]=Math.min(m(c.innerSize||0,h),h),this.translate(b),this.drawDataLabels&&this.drawDataLabels()):g=!0}return g});q.column&&(q.column.prototype.alignDataLabel=function(a,b,c,e,k){var d=
this.chart.inverted,g=a.series,h=a.dlBox||a.shapeArgs,n=x(a.below,a.plotY>x(this.translatedThreshold,g.yAxis.len)),f=x(c.inside,!!this.options.stacking);h&&(e=t(h),0>e.y&&(e.height+=e.y,e.y=0),h=e.y+e.height-g.yAxis.len,0<h&&(e.height-=h),d&&(e={x:g.yAxis.len-e.y-e.height,y:g.xAxis.len-e.x-e.width,width:e.height,height:e.width}),f||(d?(e.x+=n?0:e.width,e.width=0):(e.y+=n?e.height:0,e.height=0)));c.align=x(c.align,!d||f?"center":n?"right":"left");c.verticalAlign=x(c.verticalAlign,d||f?"middle":n?"top":
"bottom");p.prototype.alignDataLabel.call(this,a,b,c,e,k);c.inside&&a.contrastColor&&b.css({color:a.contrastColor})})});K(D,"modules/overlapping-datalabels.src.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.isArray,G=g.objectEach,H=g.pick;g=c.Chart;var y=c.addEvent,w=c.fireEvent;y(g,"render",function(){var c=[];(this.labelCollectors||[]).forEach(function(g){c=c.concat(g())});(this.yAxis||[]).forEach(function(g){g.options.stackLabels&&!g.options.stackLabels.allowOverlap&&
G(g.stacks,function(g){G(g,function(g){c.push(g.label)})})});(this.series||[]).forEach(function(g){var x=g.options.dataLabels;g.visible&&(!1!==x.enabled||g._hasPointLabels)&&g.points.forEach(function(g){g.visible&&(D(g.dataLabels)?g.dataLabels:g.dataLabel?[g.dataLabel]:[]).forEach(function(m){var p=m.options;m.labelrank=H(p.labelrank,g.labelrank,g.shapeArgs&&g.shapeArgs.height);p.allowOverlap||c.push(m)})})});this.hideOverlappingLabels(c)});g.prototype.hideOverlappingLabels=function(c){var g=this,
x=c.length,t=g.renderer,m,p,q;var h=function(a){var b=a.box?0:a.padding||0;var c=0;if(a&&(!a.alignAttr||a.placed)){var d=a.attr("x");var g=a.attr("y");d="number"===typeof d&&"number"===typeof g?{x:d,y:g}:a.alignAttr;g=a.parentGroup;a.width||(c=a.getBBox(),a.width=c.width,a.height=c.height,c=t.fontMetrics(null,a.element).h);return{x:d.x+(g.translateX||0)+b,y:d.y+(g.translateY||0)+b-c,width:a.width-2*b,height:a.height-2*b}}};for(p=0;p<x;p++)if(m=c[p])m.oldOpacity=m.opacity,m.newOpacity=1,m.absoluteBox=
h(m);c.sort(function(a,b){return(b.labelrank||0)-(a.labelrank||0)});for(p=0;p<x;p++){var a=(h=c[p])&&h.absoluteBox;for(m=p+1;m<x;++m){var b=(q=c[m])&&q.absoluteBox;!a||!b||h===q||0===h.newOpacity||0===q.newOpacity||b.x>a.x+a.width||b.x+b.width<a.x||b.y>a.y+a.height||b.y+b.height<a.y||((h.labelrank<q.labelrank?h:q).newOpacity=0)}}c.forEach(function(a){var b;if(a){var c=a.newOpacity;a.oldOpacity!==c&&(a.alignAttr&&a.placed?(c?a.show(!0):b=function(){a.hide(!0);a.placed=!1},a.alignAttr.opacity=c,a[a.isOld?
"animate":"attr"](a.alignAttr,null,b),w(g,"afterHideOverlappingLabels")):a.attr({opacity:c}));a.isOld=!0}})}});K(D,"parts/Interaction.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.defined,G=g.extend,H=g.isArray,y=g.isObject,w=g.objectEach,x=g.pick,E=c.addEvent;g=c.Chart;var F=c.createElement,t=c.css,m=c.defaultOptions,p=c.defaultPlotOptions,q=c.fireEvent,h=c.hasTouch,a=c.Legend,b=c.merge,d=c.Point,e=c.Series,k=c.seriesTypes,C=c.svg;var z=c.TrackerMixin={drawTrackerPoint:function(){var a=
this,b=a.chart,c=b.pointer,d=function(a){var b=c.getPointFromEvent(a);void 0!==b&&(c.isDirectTouch=!0,b.onMouseOver(a))},e;a.points.forEach(function(a){e=H(a.dataLabels)?a.dataLabels:a.dataLabel?[a.dataLabel]:[];a.graphic&&(a.graphic.element.point=a);e.forEach(function(b){b.div?b.div.point=a:b.element.point=a})});a._hasTracking||(a.trackerGroups.forEach(function(f){if(a[f]){a[f].addClass("highcharts-tracker").on("mouseover",d).on("mouseout",function(a){c.onTrackerMouseOut(a)});if(h)a[f].on("touchstart",
d);!b.styledMode&&a.options.cursor&&a[f].css(t).css({cursor:a.options.cursor})}}),a._hasTracking=!0);q(this,"afterDrawTracker")},drawTrackerGraph:function(){var a=this,b=a.options,c=b.trackByArea,d=[].concat(c?a.areaPath:a.graphPath),e=d.length,k=a.chart,g=k.pointer,m=k.renderer,p=k.options.tooltip.snap,t=a.tracker,z,x=function(){if(k.hoverSeries!==a)a.onMouseOver()},w="rgba(192,192,192,"+(C?.0001:.002)+")";if(e&&!c)for(z=e+1;z--;)"M"===d[z]&&d.splice(z+1,0,d[z+1]-p,d[z+2],"L"),(z&&"M"===d[z]||z===
e)&&d.splice(z,0,"L",d[z-2]+p,d[z-1]);t?t.attr({d:d}):a.graph&&(a.tracker=m.path(d).attr({visibility:a.visible?"visible":"hidden",zIndex:2}).addClass(c?"highcharts-tracker-area":"highcharts-tracker-line").add(a.group),k.styledMode||a.tracker.attr({"stroke-linejoin":"round",stroke:w,fill:c?w:"none","stroke-width":a.graph.strokeWidth()+(c?0:2*p)}),[a.tracker,a.markerGroup].forEach(function(a){a.addClass("highcharts-tracker").on("mouseover",x).on("mouseout",function(a){g.onTrackerMouseOut(a)});b.cursor&&
!k.styledMode&&a.css({cursor:b.cursor});if(h)a.on("touchstart",x)}));q(this,"afterDrawTracker")}};k.column&&(k.column.prototype.drawTracker=z.drawTrackerPoint);k.pie&&(k.pie.prototype.drawTracker=z.drawTrackerPoint);k.scatter&&(k.scatter.prototype.drawTracker=z.drawTrackerPoint);G(a.prototype,{setItemEvents:function(a,c,f){var l=this,e=l.chart.renderer.boxWrapper,k=a instanceof d,n="highcharts-legend-"+(k?"point":"series")+"-active",g=l.chart.styledMode;(f?c:a.legendGroup).on("mouseover",function(){a.visible&&
l.allItems.forEach(function(b){a!==b&&b.setState("inactive",!k)});a.setState("hover");a.visible&&e.addClass(n);g||c.css(l.options.itemHoverStyle)}).on("mouseout",function(){l.chart.styledMode||c.css(b(a.visible?l.itemStyle:l.itemHiddenStyle));l.allItems.forEach(function(b){a!==b&&b.setState("",!k)});e.removeClass(n);a.setState()}).on("click",function(b){var c=function(){a.setVisible&&a.setVisible();l.allItems.forEach(function(b){a!==b&&b.setState(a.visible?"inactive":"",!k)})};e.removeClass(n);b=
{browserEvent:b};a.firePointEvent?a.firePointEvent("legendItemClick",b,c):q(a,"legendItemClick",b,c)})},createCheckboxForItem:function(a){a.checkbox=F("input",{type:"checkbox",className:"highcharts-legend-checkbox",checked:a.selected,defaultChecked:a.selected},this.options.itemCheckboxStyle,this.chart.container);E(a.checkbox,"click",function(b){q(a.series||a,"checkboxClick",{checked:b.target.checked,item:a},function(){a.select()})})}});G(g.prototype,{showResetZoom:function(){function a(){b.zoomOut()}
var b=this,c=m.lang,d=b.options.chart.resetZoomButton,e=d.theme,k=e.states,g="chart"===d.relativeTo||"spaceBox"===d.relativeTo?null:"plotBox";q(this,"beforeShowResetZoom",null,function(){b.resetZoomButton=b.renderer.button(c.resetZoom,null,null,a,e,k&&k.hover).attr({align:d.position.align,title:c.resetZoomTitle}).addClass("highcharts-reset-zoom").add().align(d.position,!1,g)});q(this,"afterShowResetZoom")},zoomOut:function(){q(this,"selection",{resetSelection:!0},this.zoom)},zoom:function(a){var b=
this,c,d=b.pointer,e=!1,k=b.inverted?d.mouseDownX:d.mouseDownY;!a||a.resetSelection?(b.axes.forEach(function(a){c=a.zoom()}),d.initiated=!1):a.xAxis.concat(a.yAxis).forEach(function(a){var f=a.axis,l=b.inverted?f.left:f.top,n=b.inverted?l+f.width:l+f.height,g=f.isXAxis,h=!1;if(!g&&k>=l&&k<=n||g||!D(k))h=!0;d[g?"zoomX":"zoomY"]&&h&&(c=f.zoom(a.min,a.max),f.displayBtn&&(e=!0))});var g=b.resetZoomButton;e&&!g?b.showResetZoom():!e&&y(g)&&(b.resetZoomButton=g.destroy());c&&b.redraw(x(b.options.chart.animation,
a&&a.animation,100>b.pointCount))},pan:function(a,b){var c=this,d=c.hoverPoints,e;q(this,"pan",{originalEvent:a},function(){d&&d.forEach(function(a){a.setState()});("xy"===b?[1,0]:[1]).forEach(function(b){b=c[b?"xAxis":"yAxis"][0];var f=b.horiz,d=a[f?"chartX":"chartY"];f=f?"mouseDownX":"mouseDownY";var l=c[f],k=(b.pointRange||0)/2,n=b.reversed&&!c.inverted||!b.reversed&&c.inverted?-1:1,g=b.getExtremes(),h=b.toValue(l-d,!0)+k*n;n=b.toValue(l+b.len-d,!0)-k*n;var m=n<h;l=m?n:h;h=m?h:n;n=Math.min(g.dataMin,
k?g.min:b.toValue(b.toPixels(g.min)-b.minPixelPadding));k=Math.max(g.dataMax,k?g.max:b.toValue(b.toPixels(g.max)+b.minPixelPadding));m=n-l;0<m&&(h+=m,l=n);m=h-k;0<m&&(h=k,l-=m);b.series.length&&l!==g.min&&h!==g.max&&(b.setExtremes(l,h,!1,!1,{trigger:"pan"}),e=!0);c[f]=d});e&&c.redraw(!1);t(c.container,{cursor:"move"})})}});G(d.prototype,{select:function(a,b){var c=this,d=c.series,e=d.chart;this.selectedStaging=a=x(a,!c.selected);c.firePointEvent(a?"select":"unselect",{accumulate:b},function(){c.selected=
c.options.selected=a;d.options.data[d.data.indexOf(c)]=c.options;c.setState(a&&"select");b||e.getSelectedPoints().forEach(function(a){var b=a.series;a.selected&&a!==c&&(a.selected=a.options.selected=!1,b.options.data[b.data.indexOf(a)]=a.options,a.setState(e.hoverPoints&&b.options.inactiveOtherPoints?"inactive":""),a.firePointEvent("unselect"))})});delete this.selectedStaging},onMouseOver:function(a){var b=this.series.chart,c=b.pointer;a=a?c.normalize(a):c.getChartCoordinatesFromPoint(this,b.inverted);
c.runPointActions(a,this)},onMouseOut:function(){var a=this.series.chart;this.firePointEvent("mouseOut");this.series.options.inactiveOtherPoints||(a.hoverPoints||[]).forEach(function(a){a.setState()});a.hoverPoints=a.hoverPoint=null},importEvents:function(){if(!this.hasImportedEvents){var a=this,d=b(a.series.options.point,a.options).events;a.events=d;w(d,function(b,d){c.isFunction(b)&&E(a,d,b)});this.hasImportedEvents=!0}},setState:function(a,b){var c=this.series,d=this.state,e=c.options.states[a||
"normal"]||{},k=p[c.type].marker&&c.options.marker,n=k&&!1===k.enabled,g=k&&k.states&&k.states[a||"normal"]||{},h=!1===g.enabled,m=c.stateMarkerGraphic,r=this.marker||{},t=c.chart,z=c.halo,C,w=k&&c.markerAttribs;a=a||"";if(!(a===this.state&&!b||this.selected&&"select"!==a||!1===e.enabled||a&&(h||n&&!1===g.enabled)||a&&r.states&&r.states[a]&&!1===r.states[a].enabled)){this.state=a;w&&(C=c.markerAttribs(this,a));if(this.graphic){d&&this.graphic.removeClass("highcharts-point-"+d);a&&this.graphic.addClass("highcharts-point-"+
a);if(!t.styledMode){var y=c.pointAttribs(this,a);var E=x(t.options.chart.animation,e.animation);c.options.inactiveOtherPoints&&((this.dataLabels||[]).forEach(function(a){a&&a.animate({opacity:y.opacity},E)}),this.connector&&this.connector.animate({opacity:y.opacity},E));this.graphic.animate(y,E)}C&&this.graphic.animate(C,x(t.options.chart.animation,g.animation,k.animation));m&&m.hide()}else{if(a&&g){d=r.symbol||c.symbol;m&&m.currentSymbol!==d&&(m=m.destroy());if(C)if(m)m[b?"animate":"attr"]({x:C.x,
y:C.y});else d&&(c.stateMarkerGraphic=m=t.renderer.symbol(d,C.x,C.y,C.width,C.height).add(c.markerGroup),m.currentSymbol=d);!t.styledMode&&m&&m.attr(c.pointAttribs(this,a))}m&&(m[a&&this.isInside?"show":"hide"](),m.element.point=this)}a=e.halo;e=(m=this.graphic||m)&&m.visibility||"inherit";a&&a.size&&m&&"hidden"!==e?(z||(c.halo=z=t.renderer.path().add(m.parentGroup)),z.show()[b?"animate":"attr"]({d:this.haloPath(a.size)}),z.attr({"class":"highcharts-halo highcharts-color-"+x(this.colorIndex,c.colorIndex)+
(this.className?" "+this.className:""),visibility:e,zIndex:-1}),z.point=this,t.styledMode||z.attr(G({fill:this.color||c.color,"fill-opacity":a.opacity},a.attributes))):z&&z.point&&z.point.haloPath&&z.animate({d:z.point.haloPath(0)},null,z.hide);q(this,"afterSetState")}},haloPath:function(a){return this.series.chart.renderer.symbols.circle(Math.floor(this.plotX)-a,this.plotY-a,2*a,2*a)}});G(e.prototype,{onMouseOver:function(){var a=this.chart,b=a.hoverSeries;if(b&&b!==this)b.onMouseOut();this.options.events.mouseOver&&
q(this,"mouseOver");this.setState("hover");a.hoverSeries=this},onMouseOut:function(){var a=this.options,b=this.chart,c=b.tooltip,d=b.hoverPoint;b.hoverSeries=null;if(d)d.onMouseOut();this&&a.events.mouseOut&&q(this,"mouseOut");!c||this.stickyTracking||c.shared&&!this.noSharedTooltip||c.hide();b.series.forEach(function(a){a.setState("",!0)})},setState:function(a,b){var c=this,d=c.options,e=c.graph,k=d.inactiveOtherPoints,g=d.states,n=d.lineWidth,h=d.opacity,m=x(g[a||"normal"]&&g[a||"normal"].animation,
c.chart.options.chart.animation);d=0;a=a||"";if(c.state!==a&&([c.group,c.markerGroup,c.dataLabelsGroup].forEach(function(b){b&&(c.state&&b.removeClass("highcharts-series-"+c.state),a&&b.addClass("highcharts-series-"+a))}),c.state=a,!c.chart.styledMode)){if(g[a]&&!1===g[a].enabled)return;a&&(n=g[a].lineWidth||n+(g[a].lineWidthPlus||0),h=x(g[a].opacity,h));if(e&&!e.dashstyle)for(g={"stroke-width":n},e.animate(g,m);c["zone-graph-"+d];)c["zone-graph-"+d].attr(g),d+=1;k||[c.group,c.markerGroup,c.dataLabelsGroup,
c.labelBySeries].forEach(function(a){a&&a.animate({opacity:h},m)})}b&&k&&c.points&&c.setAllPointsToState(a)},setAllPointsToState:function(a){this.points.forEach(function(b){b.setState&&b.setState(a)})},setVisible:function(a,b){var c=this,d=c.chart,e=c.legendItem,k=d.options.chart.ignoreHiddenSeries,g=c.visible;var n=(c.visible=a=c.options.visible=c.userOptions.visible=void 0===a?!g:a)?"show":"hide";["group","dataLabelsGroup","markerGroup","tracker","tt"].forEach(function(a){if(c[a])c[a][n]()});if(d.hoverSeries===
c||(d.hoverPoint&&d.hoverPoint.series)===c)c.onMouseOut();e&&d.legend.colorizeItem(c,a);c.isDirty=!0;c.options.stacking&&d.series.forEach(function(a){a.options.stacking&&a.visible&&(a.isDirty=!0)});c.linkedSeries.forEach(function(b){b.setVisible(a,!1)});k&&(d.isDirtyBox=!0);q(c,n);!1!==b&&d.redraw()},show:function(){this.setVisible(!0)},hide:function(){this.setVisible(!1)},select:function(a){this.selected=a=this.options.selected=void 0===a?!this.selected:a;this.checkbox&&(this.checkbox.checked=a);
q(this,a?"select":"unselect")},drawTracker:z.drawTrackerGraph})});K(D,"parts/Responsive.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.isArray,G=g.isObject,H=g.objectEach,y=g.pick,w=g.splat;g=c.Chart;g.prototype.setResponsive=function(g,w){var x=this.options.responsive,t=[],m=this.currentResponsive;!w&&x&&x.rules&&x.rules.forEach(function(g){void 0===g._id&&(g._id=c.uniqueKey());this.matchResponsiveRule(g,t)},this);w=c.merge.apply(0,t.map(function(g){return c.find(x.rules,
function(c){return c._id===g}).chartOptions}));w.isResponsiveOptions=!0;t=t.toString()||void 0;t!==(m&&m.ruleIds)&&(m&&this.update(m.undoOptions,g,!0),t?(m=this.currentOptions(w),m.isResponsiveOptions=!0,this.currentResponsive={ruleIds:t,mergedOptions:w,undoOptions:m},this.update(w,g,!0)):this.currentResponsive=void 0)};g.prototype.matchResponsiveRule=function(c,g){var w=c.condition;(w.callback||function(){return this.chartWidth<=y(w.maxWidth,Number.MAX_VALUE)&&this.chartHeight<=y(w.maxHeight,Number.MAX_VALUE)&&
this.chartWidth>=y(w.minWidth,0)&&this.chartHeight>=y(w.minHeight,0)}).call(this)&&g.push(c._id)};g.prototype.currentOptions=function(c){function g(c,p,q,h){var a;H(c,function(b,c){if(!h&&-1<x.collectionsWithUpdate.indexOf(c))for(b=w(b),q[c]=[],a=0;a<b.length;a++)p[c][a]&&(q[c][a]={},g(b[a],p[c][a],q[c][a],h+1));else G(b)?(q[c]=D(b)?[]:{},g(b,p[c]||{},q[c],h+1)):q[c]=void 0===p[c]?null:p[c]})}var x=this,t={};g(c,this.options,t,0);return t}});K(D,"masters/highcharts.src.js",[D["parts/Globals.js"],
D["parts/Utilities.js"]],function(c,g){var D=g.extend;D(c,{arrayMax:g.arrayMax,arrayMin:g.arrayMin,attr:g.attr,defined:g.defined,erase:g.erase,extend:g.extend,isArray:g.isArray,isClass:g.isClass,isDOMElement:g.isDOMElement,isNumber:g.isNumber,isObject:g.isObject,isString:g.isString,objectEach:g.objectEach,pick:g.pick,pInt:g.pInt,setAnimation:g.setAnimation,splat:g.splat,syncTimeout:g.syncTimeout});return c});K(D,"parts/Scrollbar.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){function D(a,
b,c){this.init(a,b,c)}var G=g.defined,H=g.destroyObjectProperties,y=g.pick,w=c.addEvent;g=c.Axis;var x=c.correctFloat,E=c.defaultOptions,F=c.fireEvent,t=c.hasTouch,m=c.merge,p=c.removeEvent,q,h={height:c.isTouchDevice?20:14,barBorderRadius:0,buttonBorderRadius:0,liveRedraw:void 0,margin:10,minWidth:6,step:.2,zIndex:3,barBackgroundColor:"#cccccc",barBorderWidth:1,barBorderColor:"#cccccc",buttonArrowColor:"#333333",buttonBackgroundColor:"#e6e6e6",buttonBorderColor:"#cccccc",buttonBorderWidth:1,rifleColor:"#333333",
trackBackgroundColor:"#f2f2f2",trackBorderColor:"#f2f2f2",trackBorderWidth:1};E.scrollbar=m(!0,h,E.scrollbar);c.swapXY=q=function(a,b){var c=a.length;if(b)for(b=0;b<c;b+=3){var e=a[b+1];a[b+1]=a[b+2];a[b+2]=e}return a};D.prototype={init:function(a,b,c){this.scrollbarButtons=[];this.renderer=a;this.userOptions=b;this.options=m(h,b);this.chart=c;this.size=y(this.options.size,this.options.height);b.enabled&&(this.render(),this.initEvents(),this.addEvents())},render:function(){var a=this.renderer,b=this.options,
c=this.size,e=this.chart.styledMode,k;this.group=k=a.g("scrollbar").attr({zIndex:b.zIndex,translateY:-99999}).add();this.track=a.rect().addClass("highcharts-scrollbar-track").attr({x:0,r:b.trackBorderRadius||0,height:c,width:c}).add(k);e||this.track.attr({fill:b.trackBackgroundColor,stroke:b.trackBorderColor,"stroke-width":b.trackBorderWidth});this.trackBorderWidth=this.track.strokeWidth();this.track.attr({y:-this.trackBorderWidth%2/2});this.scrollbarGroup=a.g().add(k);this.scrollbar=a.rect().addClass("highcharts-scrollbar-thumb").attr({height:c,
width:c,r:b.barBorderRadius||0}).add(this.scrollbarGroup);this.scrollbarRifles=a.path(q(["M",-3,c/4,"L",-3,2*c/3,"M",0,c/4,"L",0,2*c/3,"M",3,c/4,"L",3,2*c/3],b.vertical)).addClass("highcharts-scrollbar-rifles").add(this.scrollbarGroup);e||(this.scrollbar.attr({fill:b.barBackgroundColor,stroke:b.barBorderColor,"stroke-width":b.barBorderWidth}),this.scrollbarRifles.attr({stroke:b.rifleColor,"stroke-width":1}));this.scrollbarStrokeWidth=this.scrollbar.strokeWidth();this.scrollbarGroup.translate(-this.scrollbarStrokeWidth%
2/2,-this.scrollbarStrokeWidth%2/2);this.drawScrollbarButton(0);this.drawScrollbarButton(1)},position:function(a,b,c,e){var d=this.options.vertical,g=0,h=this.rendered?"animate":"attr";this.x=a;this.y=b+this.trackBorderWidth;this.width=c;this.xOffset=this.height=e;this.yOffset=g;d?(this.width=this.yOffset=c=g=this.size,this.xOffset=b=0,this.barWidth=e-2*c,this.x=a+=this.options.margin):(this.height=this.xOffset=e=b=this.size,this.barWidth=c-2*e,this.y+=this.options.margin);this.group[h]({translateX:a,
translateY:this.y});this.track[h]({width:c,height:e});this.scrollbarButtons[1][h]({translateX:d?0:c-b,translateY:d?e-g:0})},drawScrollbarButton:function(a){var b=this.renderer,c=this.scrollbarButtons,e=this.options,k=this.size;var g=b.g().add(this.group);c.push(g);g=b.rect().addClass("highcharts-scrollbar-button").add(g);this.chart.styledMode||g.attr({stroke:e.buttonBorderColor,"stroke-width":e.buttonBorderWidth,fill:e.buttonBackgroundColor});g.attr(g.crisp({x:-.5,y:-.5,width:k+1,height:k+1,r:e.buttonBorderRadius},
g.strokeWidth()));g=b.path(q(["M",k/2+(a?-1:1),k/2-3,"L",k/2+(a?-1:1),k/2+3,"L",k/2+(a?2:-2),k/2],e.vertical)).addClass("highcharts-scrollbar-arrow").add(c[a]);this.chart.styledMode||g.attr({fill:e.buttonArrowColor})},setRange:function(a,b){var c=this.options,e=c.vertical,k=c.minWidth,g=this.barWidth,h,m=!this.rendered||this.hasDragged||this.chart.navigator&&this.chart.navigator.hasDragged?"attr":"animate";if(G(g)){a=Math.max(a,0);var n=Math.ceil(g*a);this.calculatedWidth=h=x(g*Math.min(b,1)-n);h<
k&&(n=(g-k+h)*a,h=k);k=Math.floor(n+this.xOffset+this.yOffset);g=h/2-.5;this.from=a;this.to=b;e?(this.scrollbarGroup[m]({translateY:k}),this.scrollbar[m]({height:h}),this.scrollbarRifles[m]({translateY:g}),this.scrollbarTop=k,this.scrollbarLeft=0):(this.scrollbarGroup[m]({translateX:k}),this.scrollbar[m]({width:h}),this.scrollbarRifles[m]({translateX:g}),this.scrollbarLeft=k,this.scrollbarTop=0);12>=h?this.scrollbarRifles.hide():this.scrollbarRifles.show(!0);!1===c.showFull&&(0>=a&&1<=b?this.group.hide():
this.group.show());this.rendered=!0}},initEvents:function(){var a=this;a.mouseMoveHandler=function(b){var c=a.chart.pointer.normalize(b),e=a.options.vertical?"chartY":"chartX",k=a.initPositions;!a.grabbedCenter||b.touches&&0===b.touches[0][e]||(c=a.cursorToScrollbarPosition(c)[e],e=a[e],e=c-e,a.hasDragged=!0,a.updatePosition(k[0]+e,k[1]+e),a.hasDragged&&F(a,"changed",{from:a.from,to:a.to,trigger:"scrollbar",DOMType:b.type,DOMEvent:b}))};a.mouseUpHandler=function(b){a.hasDragged&&F(a,"changed",{from:a.from,
to:a.to,trigger:"scrollbar",DOMType:b.type,DOMEvent:b});a.grabbedCenter=a.hasDragged=a.chartX=a.chartY=null};a.mouseDownHandler=function(b){b=a.chart.pointer.normalize(b);b=a.cursorToScrollbarPosition(b);a.chartX=b.chartX;a.chartY=b.chartY;a.initPositions=[a.from,a.to];a.grabbedCenter=!0};a.buttonToMinClick=function(b){var c=x(a.to-a.from)*a.options.step;a.updatePosition(x(a.from-c),x(a.to-c));F(a,"changed",{from:a.from,to:a.to,trigger:"scrollbar",DOMEvent:b})};a.buttonToMaxClick=function(b){var c=
(a.to-a.from)*a.options.step;a.updatePosition(a.from+c,a.to+c);F(a,"changed",{from:a.from,to:a.to,trigger:"scrollbar",DOMEvent:b})};a.trackClick=function(b){var c=a.chart.pointer.normalize(b),e=a.to-a.from,k=a.y+a.scrollbarTop,g=a.x+a.scrollbarLeft;a.options.vertical&&c.chartY>k||!a.options.vertical&&c.chartX>g?a.updatePosition(a.from+e,a.to+e):a.updatePosition(a.from-e,a.to-e);F(a,"changed",{from:a.from,to:a.to,trigger:"scrollbar",DOMEvent:b})}},cursorToScrollbarPosition:function(a){var b=this.options;
b=b.minWidth>this.calculatedWidth?b.minWidth:0;return{chartX:(a.chartX-this.x-this.xOffset)/(this.barWidth-b),chartY:(a.chartY-this.y-this.yOffset)/(this.barWidth-b)}},updatePosition:function(a,b){1<b&&(a=x(1-x(b-a)),b=1);0>a&&(b=x(b-a),a=0);this.from=a;this.to=b},update:function(a){this.destroy();this.init(this.chart.renderer,m(!0,this.options,a),this.chart)},addEvents:function(){var a=this.options.inverted?[1,0]:[0,1],b=this.scrollbarButtons,c=this.scrollbarGroup.element,e=this.mouseDownHandler,
k=this.mouseMoveHandler,g=this.mouseUpHandler;a=[[b[a[0]].element,"click",this.buttonToMinClick],[b[a[1]].element,"click",this.buttonToMaxClick],[this.track.element,"click",this.trackClick],[c,"mousedown",e],[c.ownerDocument,"mousemove",k],[c.ownerDocument,"mouseup",g]];t&&a.push([c,"touchstart",e],[c.ownerDocument,"touchmove",k],[c.ownerDocument,"touchend",g]);a.forEach(function(a){w.apply(null,a)});this._events=a},removeEvents:function(){this._events.forEach(function(a){p.apply(null,a)});this._events.length=
0},destroy:function(){var a=this.chart.scroller;this.removeEvents();["track","scrollbarRifles","scrollbar","scrollbarGroup","group"].forEach(function(a){this[a]&&this[a].destroy&&(this[a]=this[a].destroy())},this);a&&this===a.scrollbar&&(a.scrollbar=null,H(a.scrollbarButtons))}};c.Scrollbar||(w(g,"afterInit",function(){var a=this;a.options&&a.options.scrollbar&&a.options.scrollbar.enabled&&(a.options.scrollbar.vertical=!a.horiz,a.options.startOnTick=a.options.endOnTick=!1,a.scrollbar=new D(a.chart.renderer,
a.options.scrollbar,a.chart),w(a.scrollbar,"changed",function(b){var d=Math.min(y(a.options.min,a.min),a.min,a.dataMin),e=Math.max(y(a.options.max,a.max),a.max,a.dataMax)-d;if(a.horiz&&!a.reversed||!a.horiz&&a.reversed){var k=d+e*this.to;d+=e*this.from}else k=d+e*(1-this.from),d+=e*(1-this.to);y(this.options.liveRedraw,c.svg&&!c.isTouchDevice&&!this.chart.isBoosting)||"mouseup"===b.DOMType||!G(b.DOMType)?a.setExtremes(d,k,!0,"mousemove"!==b.DOMType,b):this.setRange(this.from,this.to)}))}),w(g,"afterRender",
function(){var a=Math.min(y(this.options.min,this.min),this.min,y(this.dataMin,this.min)),b=Math.max(y(this.options.max,this.max),this.max,y(this.dataMax,this.max)),c=this.scrollbar,e=this.axisTitleMargin+(this.titleOffset||0),k=this.chart.scrollbarsOffsets,g=this.options.margin||0;c&&(this.horiz?(this.opposite||(k[1]+=e),c.position(this.left,this.top+this.height+2+k[1]-(this.opposite?g:0),this.width,this.height),this.opposite||(k[1]+=g),e=1):(this.opposite&&(k[0]+=e),c.position(this.left+this.width+
2+k[0]-(this.opposite?0:g),this.top,this.width,this.height),this.opposite&&(k[0]+=g),e=0),k[e]+=c.size+c.options.margin,isNaN(a)||isNaN(b)||!G(this.min)||!G(this.max)||this.min===this.max?c.setRange(0,1):(k=(this.min-a)/(b-a),a=(this.max-a)/(b-a),this.horiz&&!this.reversed||!this.horiz&&this.reversed?c.setRange(k,a):c.setRange(1-a,1-k)))}),w(g,"afterGetOffset",function(){var a=this.horiz?2:1,b=this.scrollbar;b&&(this.chart.scrollbarsOffsets=[0,0],this.chart.axisOffset[a]+=b.size+b.options.margin)}),
c.Scrollbar=D)});K(D,"parts/Navigator.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){function D(a){this.init(a)}var G=g.defined,H=g.destroyObjectProperties,y=g.erase,w=g.extend,x=g.isArray,E=g.isNumber,F=g.pick,t=g.splat,m=c.addEvent,p=c.Axis;g=c.Chart;var q=c.color,h=c.defaultOptions,a=c.hasTouch,b=c.isTouchDevice,d=c.merge,e=c.removeEvent,k=c.Scrollbar,C=c.Series,z=function(a){for(var b=[],c=1;c<arguments.length;c++)b[c-1]=arguments[c];b=[].filter.call(b,E);if(b.length)return Math[a].apply(0,
b)};var r=void 0===c.seriesTypes.areaspline?"line":"areaspline";w(h,{navigator:{height:40,margin:25,maskInside:!0,handles:{width:7,height:15,symbols:["navigator-handle","navigator-handle"],enabled:!0,lineWidth:1,backgroundColor:"#f2f2f2",borderColor:"#999999"},maskFill:q("#6685c2").setOpacity(.3).get(),outlineColor:"#cccccc",outlineWidth:1,series:{type:r,fillOpacity:.05,lineWidth:1,compare:null,dataGrouping:{approximation:"average",enabled:!0,groupPixelWidth:2,smoothed:!0,units:[["millisecond",[1,
2,5,10,20,25,50,100,200,500]],["second",[1,2,5,10,15,30]],["minute",[1,2,5,10,15,30]],["hour",[1,2,3,4,6,8,12]],["day",[1,2,3,4]],["week",[1,2,3]],["month",[1,3,6]],["year",null]]},dataLabels:{enabled:!1,zIndex:2},id:"highcharts-navigator-series",className:"highcharts-navigator-series",lineColor:null,marker:{enabled:!1},threshold:null},xAxis:{overscroll:0,className:"highcharts-navigator-xaxis",tickLength:0,lineWidth:0,gridLineColor:"#e6e6e6",gridLineWidth:1,tickPixelInterval:200,labels:{align:"left",
style:{color:"#999999"},x:3,y:-4},crosshair:!1},yAxis:{className:"highcharts-navigator-yaxis",gridLineWidth:0,startOnTick:!1,endOnTick:!1,minPadding:.1,maxPadding:.1,labels:{enabled:!1},crosshair:!1,title:{text:null},tickLength:0,tickWidth:0}}});c.Renderer.prototype.symbols["navigator-handle"]=function(a,b,c,d,e){a=e.width/2;b=Math.round(a/3)+.5;e=e.height;return["M",-a-1,.5,"L",a,.5,"L",a,e+.5,"L",-a-1,e+.5,"L",-a-1,.5,"M",-b,4,"L",-b,e-3,"M",b-1,4,"L",b-1,e-3]};p.prototype.toFixedRange=function(a,
b,d,e){var f=this.chart&&this.chart.fixedRange,l=(this.pointRange||0)/2;a=F(d,this.translate(a,!0,!this.horiz));b=F(e,this.translate(b,!0,!this.horiz));var k=f&&(b-a)/f;G(d)||(a=c.correctFloat(a+l));G(e)||(b=c.correctFloat(b-l));.7<k&&1.3>k&&(e?a=b-f:b=a+f);E(a)&&E(b)||(a=b=void 0);return{min:a,max:b}};D.prototype={drawHandle:function(a,b,c,d){var f=this.navigatorOptions.handles.height;this.handles[b][d](c?{translateX:Math.round(this.left+this.height/2),translateY:Math.round(this.top+parseInt(a,10)+
.5-f)}:{translateX:Math.round(this.left+parseInt(a,10)),translateY:Math.round(this.top+this.height/2-f/2-1)})},drawOutline:function(a,b,c,d){var f=this.navigatorOptions.maskInside,l=this.outline.strokeWidth(),e=l/2;l=l%2/2;var k=this.outlineHeight,g=this.scrollbarHeight,n=this.size,h=this.left-g,m=this.top;c?(h-=e,c=m+b+l,b=m+a+l,a=["M",h+k,m-g-l,"L",h+k,c,"L",h,c,"L",h,b,"L",h+k,b,"L",h+k,m+n+g].concat(f?["M",h+k,c-e,"L",h+k,b+e]:[])):(a+=h+g-l,b+=h+g-l,m+=e,a=["M",h,m,"L",a,m,"L",a,m+k,"L",b,m+
k,"L",b,m,"L",h+n+2*g,m].concat(f?["M",a-e,m,"L",b+e,m]:[]));this.outline[d]({d:a})},drawMasks:function(a,b,c,d){var f=this.left,l=this.top,e=this.height;if(c){var k=[f,f,f];var g=[l,l+a,l+b];var n=[e,e,e];var h=[a,b-a,this.size-b]}else k=[f,f+a,f+b],g=[l,l,l],n=[a,b-a,this.size-b],h=[e,e,e];this.shades.forEach(function(a,b){a[d]({x:k[b],y:g[b],width:n[b],height:h[b]})})},renderElements:function(){var a=this,b=a.navigatorOptions,c=b.maskInside,d=a.chart,e=d.renderer,k,g={cursor:d.inverted?"ns-resize":
"ew-resize"};a.navigatorGroup=k=e.g("navigator").attr({zIndex:8,visibility:"hidden"}).add();[!c,c,!c].forEach(function(c,f){a.shades[f]=e.rect().addClass("highcharts-navigator-mask"+(1===f?"-inside":"-outside")).add(k);d.styledMode||a.shades[f].attr({fill:c?b.maskFill:"rgba(0,0,0,0)"}).css(1===f&&g)});a.outline=e.path().addClass("highcharts-navigator-outline").add(k);d.styledMode||a.outline.attr({"stroke-width":b.outlineWidth,stroke:b.outlineColor});b.handles.enabled&&[0,1].forEach(function(c){b.handles.inverted=
d.inverted;a.handles[c]=e.symbol(b.handles.symbols[c],-b.handles.width/2-1,0,b.handles.width,b.handles.height,b.handles);a.handles[c].attr({zIndex:7-c}).addClass("highcharts-navigator-handle highcharts-navigator-handle-"+["left","right"][c]).add(k);if(!d.styledMode){var f=b.handles;a.handles[c].attr({fill:f.backgroundColor,stroke:f.borderColor,"stroke-width":f.lineWidth}).css(g)}})},update:function(a){(this.series||[]).forEach(function(a){a.baseSeries&&delete a.baseSeries.navigatorSeries});this.destroy();
d(!0,this.chart.options.navigator,this.options,a);this.init(this.chart)},render:function(a,b,d,e){var f=this.chart,l=this.scrollbarHeight,k,g=this.xAxis,h=g.pointRange||0;var n=g.fake?f.xAxis[0]:g;var m=this.navigatorEnabled,r,v=this.rendered;var p=f.inverted;var q=f.xAxis[0].minRange,t=f.xAxis[0].options.maxRange;if(!this.hasDragged||G(d)){a=c.correctFloat(a-h/2);b=c.correctFloat(b+h/2);if(!E(a)||!E(b))if(v)d=0,e=F(g.width,n.width);else return;this.left=F(g.left,f.plotLeft+l+(p?f.plotWidth:0));this.size=
r=k=F(g.len,(p?f.plotHeight:f.plotWidth)-2*l);f=p?l:k+2*l;d=F(d,g.toPixels(a,!0));e=F(e,g.toPixels(b,!0));E(d)&&Infinity!==Math.abs(d)||(d=0,e=f);a=g.toValue(d,!0);b=g.toValue(e,!0);var z=Math.abs(c.correctFloat(b-a));c.correctFloat(z-h)<q?this.grabbedLeft?d=g.toPixels(b-q-h,!0):this.grabbedRight&&(e=g.toPixels(a+q+h,!0)):G(t)&&c.correctFloat(z-h)>t&&(this.grabbedLeft?d=g.toPixels(b-t-h,!0):this.grabbedRight&&(e=g.toPixels(a+t+h,!0)));this.zoomedMax=Math.min(Math.max(d,e,0),r);this.zoomedMin=Math.min(Math.max(this.fixedWidth?
this.zoomedMax-this.fixedWidth:Math.min(d,e),0),r);this.range=this.zoomedMax-this.zoomedMin;r=Math.round(this.zoomedMax);d=Math.round(this.zoomedMin);m&&(this.navigatorGroup.attr({visibility:"visible"}),v=v&&!this.hasDragged?"animate":"attr",this.drawMasks(d,r,p,v),this.drawOutline(d,r,p,v),this.navigatorOptions.handles.enabled&&(this.drawHandle(d,0,p,v),this.drawHandle(r,1,p,v)));this.scrollbar&&(p?(p=this.top-l,n=this.left-l+(m||!n.opposite?0:(n.titleOffset||0)+n.axisTitleMargin),l=k+2*l):(p=this.top+
(m?this.height:-l),n=this.left-l),this.scrollbar.position(n,p,f,l),this.scrollbar.setRange(this.zoomedMin/(k||1),this.zoomedMax/(k||1)));this.rendered=!0}},addMouseEvents:function(){var b=this,c=b.chart,d=c.container,e=[],k,g;b.mouseMoveHandler=k=function(a){b.onMouseMove(a)};b.mouseUpHandler=g=function(a){b.onMouseUp(a)};e=b.getPartsEvents("mousedown");e.push(m(c.renderTo,"mousemove",k),m(d.ownerDocument,"mouseup",g));a&&(e.push(m(c.renderTo,"touchmove",k),m(d.ownerDocument,"touchend",g)),e.concat(b.getPartsEvents("touchstart")));
b.eventsToUnbind=e;b.series&&b.series[0]&&e.push(m(b.series[0].xAxis,"foundExtremes",function(){c.navigator.modifyNavigatorAxisExtremes()}))},getPartsEvents:function(a){var b=this,c=[];["shades","handles"].forEach(function(d){b[d].forEach(function(f,l){c.push(m(f.element,a,function(a){b[d+"Mousedown"](a,l)}))})});return c},shadesMousedown:function(a,b){a=this.chart.pointer.normalize(a);var c=this.chart,d=this.xAxis,f=this.zoomedMin,e=this.left,k=this.size,g=this.range,h=a.chartX;c.inverted&&(h=a.chartY,
e=this.top);if(1===b)this.grabbedCenter=h,this.fixedWidth=g,this.dragOffset=h-f;else{a=h-e-g/2;if(0===b)a=Math.max(0,a);else if(2===b&&a+g>=k)if(a=k-g,this.reversedExtremes){a-=g;var n=this.getUnionExtremes().dataMin}else var m=this.getUnionExtremes().dataMax;a!==f&&(this.fixedWidth=g,b=d.toFixedRange(a,a+g,n,m),G(b.min)&&c.xAxis[0].setExtremes(Math.min(b.min,b.max),Math.max(b.min,b.max),!0,null,{trigger:"navigator"}))}},handlesMousedown:function(a,b){this.chart.pointer.normalize(a);a=this.chart;
var c=a.xAxis[0],d=this.reversedExtremes;0===b?(this.grabbedLeft=!0,this.otherHandlePos=this.zoomedMax,this.fixedExtreme=d?c.min:c.max):(this.grabbedRight=!0,this.otherHandlePos=this.zoomedMin,this.fixedExtreme=d?c.max:c.min);a.fixedRange=null},onMouseMove:function(a){var d=this,e=d.chart,k=d.left,g=d.navigatorSize,h=d.range,m=d.dragOffset,n=e.inverted;a.touches&&0===a.touches[0].pageX||(a=e.pointer.normalize(a),e=a.chartX,n&&(k=d.top,e=a.chartY),d.grabbedLeft?(d.hasDragged=!0,d.render(0,0,e-k,d.otherHandlePos)):
d.grabbedRight?(d.hasDragged=!0,d.render(0,0,d.otherHandlePos,e-k)):d.grabbedCenter&&(d.hasDragged=!0,e<m?e=m:e>g+m-h&&(e=g+m-h),d.render(0,0,e-m,e-m+h)),d.hasDragged&&d.scrollbar&&F(d.scrollbar.options.liveRedraw,c.svg&&!b&&!this.chart.isBoosting)&&(a.DOMType=a.type,setTimeout(function(){d.onMouseUp(a)},0)))},onMouseUp:function(a){var b=this.chart,c=this.xAxis,d=this.scrollbar,e=a.DOMEvent||a;if(this.hasDragged&&(!d||!d.hasDragged)||"scrollbar"===a.trigger){d=this.getUnionExtremes();if(this.zoomedMin===
this.otherHandlePos)var k=this.fixedExtreme;else if(this.zoomedMax===this.otherHandlePos)var g=this.fixedExtreme;this.zoomedMax===this.size&&(g=this.reversedExtremes?d.dataMin:d.dataMax);0===this.zoomedMin&&(k=this.reversedExtremes?d.dataMax:d.dataMin);c=c.toFixedRange(this.zoomedMin,this.zoomedMax,k,g);G(c.min)&&b.xAxis[0].setExtremes(Math.min(c.min,c.max),Math.max(c.min,c.max),!0,this.hasDragged?!1:null,{trigger:"navigator",triggerOp:"navigator-drag",DOMEvent:e})}"mousemove"!==a.DOMType&&"touchmove"!==
a.DOMType&&(this.grabbedLeft=this.grabbedRight=this.grabbedCenter=this.fixedWidth=this.fixedExtreme=this.otherHandlePos=this.hasDragged=this.dragOffset=null)},removeEvents:function(){this.eventsToUnbind&&(this.eventsToUnbind.forEach(function(a){a()}),this.eventsToUnbind=void 0);this.removeBaseSeriesEvents()},removeBaseSeriesEvents:function(){var a=this.baseSeries||[];this.navigatorEnabled&&a[0]&&(!1!==this.navigatorOptions.adaptToUpdatedData&&a.forEach(function(a){e(a,"updatedData",this.updatedDataHandler)},
this),a[0].xAxis&&e(a[0].xAxis,"foundExtremes",this.modifyBaseAxisExtremes))},init:function(a){var b=a.options,c=b.navigator,e=c.enabled,g=b.scrollbar,h=g.enabled;b=e?c.height:0;var n=h?g.height:0;this.handles=[];this.shades=[];this.chart=a;this.setBaseSeries();this.height=b;this.scrollbarHeight=n;this.scrollbarEnabled=h;this.navigatorEnabled=e;this.navigatorOptions=c;this.scrollbarOptions=g;this.outlineHeight=b+n;this.opposite=F(c.opposite,!(e||!a.inverted));var r=this;e=r.baseSeries;g=a.xAxis.length;
h=a.yAxis.length;var q=e&&e[0]&&e[0].xAxis||a.xAxis[0]||{options:{}};a.isDirtyBox=!0;r.navigatorEnabled?(r.xAxis=new p(a,d({breaks:q.options.breaks,ordinal:q.options.ordinal},c.xAxis,{id:"navigator-x-axis",yAxis:"navigator-y-axis",isX:!0,type:"datetime",index:g,isInternal:!0,offset:0,keepOrdinalPadding:!0,startOnTick:!1,endOnTick:!1,minPadding:0,maxPadding:0,zoomEnabled:!1},a.inverted?{offsets:[n,0,-n,0],width:b}:{offsets:[0,-n,0,n],height:b})),r.yAxis=new p(a,d(c.yAxis,{id:"navigator-y-axis",alignTicks:!1,
offset:0,index:h,isInternal:!0,zoomEnabled:!1},a.inverted?{width:b}:{height:b})),e||c.series.data?r.updateNavigatorSeries(!1):0===a.series.length&&(r.unbindRedraw=m(a,"beforeRedraw",function(){0<a.series.length&&!r.series&&(r.setBaseSeries(),r.unbindRedraw())})),r.reversedExtremes=a.inverted&&!r.xAxis.reversed||!a.inverted&&r.xAxis.reversed,r.renderElements(),r.addMouseEvents()):r.xAxis={translate:function(b,c){var d=a.xAxis[0],f=d.getExtremes(),e=d.len-2*n,l=z("min",d.options.min,f.dataMin);d=z("max",
d.options.max,f.dataMax)-l;return c?b*d/e+l:e*(b-l)/d},toPixels:function(a){return this.translate(a)},toValue:function(a){return this.translate(a,!0)},toFixedRange:p.prototype.toFixedRange,fake:!0};a.options.scrollbar.enabled&&(a.scrollbar=r.scrollbar=new k(a.renderer,d(a.options.scrollbar,{margin:r.navigatorEnabled?0:10,vertical:a.inverted}),a),m(r.scrollbar,"changed",function(b){var c=r.size,d=c*this.to;c*=this.from;r.hasDragged=r.scrollbar.hasDragged;r.render(0,0,c,d);(a.options.scrollbar.liveRedraw||
"mousemove"!==b.DOMType&&"touchmove"!==b.DOMType)&&setTimeout(function(){r.onMouseUp(b)})}));r.addBaseSeriesEvents();r.addChartEvents()},getUnionExtremes:function(a){var b=this.chart.xAxis[0],c=this.xAxis,d=c.options,e=b.options,k;a&&null===b.dataMin||(k={dataMin:F(d&&d.min,z("min",e.min,b.dataMin,c.dataMin,c.min)),dataMax:F(d&&d.max,z("max",e.max,b.dataMax,c.dataMax,c.max))});return k},setBaseSeries:function(a,b){var d=this.chart,f=this.baseSeries=[];a=a||d.options&&d.options.navigator.baseSeries||
(d.series.length?c.find(d.series,function(a){return!a.options.isInternal}).index:0);(d.series||[]).forEach(function(b,c){b.options.isInternal||!b.options.showInNavigator&&(c!==a&&b.options.id!==a||!1===b.options.showInNavigator)||f.push(b)});this.xAxis&&!this.xAxis.fake&&this.updateNavigatorSeries(!0,b)},updateNavigatorSeries:function(a,b){var c=this,f=c.chart,k=c.baseSeries,g,m,n=c.navigatorOptions.series,r,p={enableMouseTracking:!1,index:null,linkedTo:null,group:"nav",padXAxis:!1,xAxis:"navigator-x-axis",
yAxis:"navigator-y-axis",showInLegend:!1,stacking:!1,isInternal:!0,states:{inactive:{opacity:1}}},q=c.series=(c.series||[]).filter(function(a){var b=a.baseSeries;return 0>k.indexOf(b)?(b&&(e(b,"updatedData",c.updatedDataHandler),delete b.navigatorSeries),a.chart&&a.destroy(),!1):!0});k&&k.length&&k.forEach(function(a){var e=a.navigatorSeries,l=w({color:a.color,visible:a.visible},x(n)?h.navigator.series:n);e&&!1===c.navigatorOptions.adaptToUpdatedData||(p.name="Navigator "+k.length,g=a.options||{},
r=g.navigatorOptions||{},m=d(g,p,l,r),m.pointRange=F(l.pointRange,r.pointRange,h.plotOptions[m.type||"line"].pointRange),l=r.data||l.data,c.hasNavigatorData=c.hasNavigatorData||!!l,m.data=l||g.data&&g.data.slice(0),e&&e.options?e.update(m,b):(a.navigatorSeries=f.initSeries(m),a.navigatorSeries.baseSeries=a,q.push(a.navigatorSeries)))});if(n.data&&(!k||!k.length)||x(n))c.hasNavigatorData=!1,n=t(n),n.forEach(function(a,b){p.name="Navigator "+(q.length+1);m=d(h.navigator.series,{color:f.series[b]&&!f.series[b].options.isInternal&&
f.series[b].color||f.options.colors[b]||f.options.colors[0]},p,a);m.data=a.data;m.data&&(c.hasNavigatorData=!0,q.push(f.initSeries(m)))});a&&this.addBaseSeriesEvents()},addBaseSeriesEvents:function(){var a=this,b=a.baseSeries||[];b[0]&&b[0].xAxis&&m(b[0].xAxis,"foundExtremes",this.modifyBaseAxisExtremes);b.forEach(function(b){m(b,"show",function(){this.navigatorSeries&&this.navigatorSeries.setVisible(!0,!1)});m(b,"hide",function(){this.navigatorSeries&&this.navigatorSeries.setVisible(!1,!1)});!1!==
this.navigatorOptions.adaptToUpdatedData&&b.xAxis&&m(b,"updatedData",this.updatedDataHandler);m(b,"remove",function(){this.navigatorSeries&&(y(a.series,this.navigatorSeries),G(this.navigatorSeries.options)&&this.navigatorSeries.remove(!1),delete this.navigatorSeries)})},this)},getBaseSeriesMin:function(a){return this.baseSeries.reduce(function(a,b){return Math.min(a,b.xData?b.xData[0]:a)},a)},modifyNavigatorAxisExtremes:function(){var a=this.xAxis,b;"undefined"!==typeof a.getExtremes&&(!(b=this.getUnionExtremes(!0))||
b.dataMin===a.min&&b.dataMax===a.max||(a.min=b.dataMin,a.max=b.dataMax))},modifyBaseAxisExtremes:function(){var a=this.chart.navigator,b=this.getExtremes(),c=b.dataMin,d=b.dataMax;b=b.max-b.min;var e=a.stickToMin,k=a.stickToMax,g=F(this.options.overscroll,0),h=a.series&&a.series[0],m=!!this.setExtremes;if(!this.eventArgs||"rangeSelectorButton"!==this.eventArgs.trigger){if(e){var r=c;var p=r+b}k&&(p=d+g,e||(r=Math.max(p-b,a.getBaseSeriesMin(h&&h.xData?h.xData[0]:-Number.MAX_VALUE))));m&&(e||k)&&E(r)&&
(this.min=this.userMin=r,this.max=this.userMax=p)}a.stickToMin=a.stickToMax=null},updatedDataHandler:function(){var a=this.chart.navigator,b=this.navigatorSeries,c=a.getBaseSeriesMin(this.xData[0]);a.stickToMax=a.reversedExtremes?0===Math.round(a.zoomedMin):Math.round(a.zoomedMax)>=Math.round(a.size);a.stickToMin=E(this.xAxis.min)&&this.xAxis.min<=c&&(!this.chart.fixedRange||!a.stickToMax);b&&!a.hasNavigatorData&&(b.options.pointStart=this.xData[0],b.setData(this.options.data,!1,null,!1))},addChartEvents:function(){this.eventsToUnbind||
(this.eventsToUnbind=[]);this.eventsToUnbind.push(m(this.chart,"redraw",function(){var a=this.navigator,b=a&&(a.baseSeries&&a.baseSeries[0]&&a.baseSeries[0].xAxis||a.scrollbar&&this.xAxis[0]);b&&a.render(b.min,b.max)}),m(this.chart,"getMargins",function(){var a=this.navigator,b=a.opposite?"plotTop":"marginBottom";this.inverted&&(b=a.opposite?"marginRight":"plotLeft");this[b]=(this[b]||0)+(a.navigatorEnabled||!this.inverted?a.outlineHeight:0)+a.navigatorOptions.margin}))},destroy:function(){this.removeEvents();
this.xAxis&&(y(this.chart.xAxis,this.xAxis),y(this.chart.axes,this.xAxis));this.yAxis&&(y(this.chart.yAxis,this.yAxis),y(this.chart.axes,this.yAxis));(this.series||[]).forEach(function(a){a.destroy&&a.destroy()});"series xAxis yAxis shades outline scrollbarTrack scrollbarRifles scrollbarGroup scrollbar navigatorGroup rendered".split(" ").forEach(function(a){this[a]&&this[a].destroy&&this[a].destroy();this[a]=null},this);[this.handles].forEach(function(a){H(a)},this)}};c.Navigator||(c.Navigator=D,
m(p,"zoom",function(a){var c=this.chart.options,d=c.chart.zoomType,e=c.chart.pinchType,k=c.navigator;c=c.rangeSelector;this.isXAxis&&(k&&k.enabled||c&&c.enabled)&&("y"===d?a.zoomed=!1:(!b&&"xy"===d||b&&"xy"===e)&&this.options.range&&(d=this.previousZoom,G(a.newMin)?this.previousZoom=[this.min,this.max]:d&&(a.newMin=d[0],a.newMax=d[1],delete this.previousZoom)));void 0!==a.zoomed&&a.preventDefault()}),m(g,"beforeShowResetZoom",function(){var a=this.options,c=a.navigator,d=a.rangeSelector;if((c&&c.enabled||
d&&d.enabled)&&(!b&&"x"===a.chart.zoomType||b&&"x"===a.chart.pinchType))return!1}),m(g,"beforeRender",function(){var a=this.options;if(a.navigator.enabled||a.scrollbar.enabled)this.scroller=this.navigator=new D(this)}),m(g,"afterSetChartSize",function(){var a=this.legend,b=this.navigator;if(b){var c=a&&a.options;var d=b.xAxis;var e=b.yAxis;var k=b.scrollbarHeight;this.inverted?(b.left=b.opposite?this.chartWidth-k-b.height:this.spacing[3]+k,b.top=this.plotTop+k):(b.left=this.plotLeft+k,b.top=b.navigatorOptions.top||
this.chartHeight-b.height-k-this.spacing[2]-(this.rangeSelector&&this.extraBottomMargin?this.rangeSelector.getHeight():0)-(c&&"bottom"===c.verticalAlign&&c.enabled&&!c.floating?a.legendHeight+F(c.margin,10):0)-(this.titleOffset?this.titleOffset[2]:0));d&&e&&(this.inverted?d.options.left=e.options.left=b.left:d.options.top=e.options.top=b.top,d.setAxisSize(),e.setAxisSize())}}),m(g,"update",function(a){var b=a.options.navigator||{},c=a.options.scrollbar||{};this.navigator||this.scroller||!b.enabled&&
!c.enabled||(d(!0,this.options.navigator,b),d(!0,this.options.scrollbar,c),delete a.options.navigator,delete a.options.scrollbar)}),m(g,"afterUpdate",function(a){this.navigator||this.scroller||!this.options.navigator.enabled&&!this.options.scrollbar.enabled||(this.scroller=this.navigator=new D(this),F(a.redraw,!0)&&this.redraw(a.animation))}),m(g,"afterAddSeries",function(){this.navigator&&this.navigator.setBaseSeries(null,!1)}),m(C,"afterUpdate",function(){this.chart.navigator&&!this.options.isInternal&&
this.chart.navigator.setBaseSeries(null,!1)}),g.prototype.callbacks.push(function(a){var b=a.navigator;b&&a.xAxis[0]&&(a=a.xAxis[0].getExtremes(),b.render(a.min,a.max))}))});K(D,"parts/OrdinalAxis.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.defined,G=g.extend,H=g.pick;g=c.addEvent;var y=c.Axis,w=c.Chart,x=c.css,E=c.noop,F=c.timeUnits;g(c.Series,"updatedData",function(){var c=this.xAxis;c&&c.options.ordinal&&delete c.ordinalIndex});y.prototype.getTimeTicks=function(c,
g,p,q,h,a,b){var d=0,e,k,m={},t=[],r=-Number.MAX_VALUE,n=this.options.tickPixelInterval,f=this.chart.time,l=[];if(!this.options.ordinal&&!this.options.breaks||!h||3>h.length||void 0===g)return f.getTimeTicks.apply(f,arguments);var v=h.length;for(e=0;e<v;e++){var B=e&&h[e-1]>p;h[e]<g&&(d=e);if(e===v-1||h[e+1]-h[e]>5*a||B){if(h[e]>r){for(k=f.getTimeTicks(c,h[d],h[e],q);k.length&&k[0]<=r;)k.shift();k.length&&(r=k[k.length-1]);l.push(t.length);t=t.concat(k)}d=e+1}if(B)break}k=k.info;if(b&&k.unitRange<=
F.hour){e=t.length-1;for(d=1;d<e;d++)if(f.dateFormat("%d",t[d])!==f.dateFormat("%d",t[d-1])){m[t[d]]="day";var A=!0}A&&(m[t[0]]="day");k.higherRanks=m}k.segmentStarts=l;t.info=k;if(b&&D(n)){d=l=t.length;A=[];var u;for(f=[];d--;)e=this.translate(t[d]),u&&(f[d]=u-e),A[d]=u=e;f.sort();f=f[Math.floor(f.length/2)];f<.6*n&&(f=null);d=t[l-1]>p?l-1:l;for(u=void 0;d--;)e=A[d],l=Math.abs(u-e),u&&l<.8*n&&(null===f||l<.8*f)?(m[t[d]]&&!m[t[d+1]]?(l=d+1,u=e):l=d,t.splice(l,1)):u=e}return t};G(y.prototype,{beforeSetTickPositions:function(){var c=
[],g,p=!1,q=this.getExtremes(),h=q.min,a=q.max,b,d=this.isXAxis&&!!this.options.breaks;q=this.options.ordinal;var e=Number.MAX_VALUE,k=this.chart.options.chart.ignoreHiddenSeries,C;if(q||d){this.series.forEach(function(a,b){g=[];if(!(k&&!1===a.visible||!1===a.takeOrdinalPosition&&!d)&&(c=c.concat(a.processedXData),z=c.length,c.sort(function(a,b){return a-b}),e=Math.min(e,H(a.closestPointRange,e)),z)){for(b=0;b<z-1;)c[b]!==c[b+1]&&g.push(c[b+1]),b++;g[0]!==c[0]&&g.unshift(c[0]);c=g}a.isSeriesBoosting&&
(C=!0)});C&&(c.length=0);var z=c.length;if(2<z){var r=c[1]-c[0];for(b=z-1;b--&&!p;)c[b+1]-c[b]!==r&&(p=!0);!this.options.keepOrdinalPadding&&(c[0]-h>r||a-c[c.length-1]>r)&&(p=!0)}else this.options.overscroll&&(2===z?e=c[1]-c[0]:1===z?(e=this.options.overscroll,c=[c[0],c[0]+e]):e=this.overscrollPointsRange);p?(this.options.overscroll&&(this.overscrollPointsRange=e,c=c.concat(this.getOverscrollPositions())),this.ordinalPositions=c,r=this.ordinal2lin(Math.max(h,c[0]),!0),b=Math.max(this.ordinal2lin(Math.min(a,
c[c.length-1]),!0),1),this.ordinalSlope=a=(a-h)/(b-r),this.ordinalOffset=h-r*a):(this.overscrollPointsRange=H(this.closestPointRange,this.overscrollPointsRange),this.ordinalPositions=this.ordinalSlope=this.ordinalOffset=void 0)}this.isOrdinal=q&&p;this.groupIntervalFactor=null},val2lin:function(c,g){var m=this.ordinalPositions;if(m){var q=m.length,h;for(h=q;h--;)if(m[h]===c){var a=h;break}for(h=q-1;h--;)if(c>m[h]||0===h){c=(c-m[h])/(m[h+1]-m[h]);a=h+c;break}g=g?a:this.ordinalSlope*(a||0)+this.ordinalOffset}else g=
c;return g},lin2val:function(c,g){var m=this.ordinalPositions;if(m){var q=this.ordinalSlope,h=this.ordinalOffset,a=m.length-1;if(g)if(0>c)c=m[0];else if(c>a)c=m[a];else{a=Math.floor(c);var b=c-a}else for(;a--;)if(g=q*a+h,c>=g){q=q*(a+1)+h;b=(c-g)/(q-g);break}return void 0!==b&&void 0!==m[a]?m[a]+(b?b*(m[a+1]-m[a]):0):c}return c},getExtendedPositions:function(){var c=this,g=c.chart,p=c.series[0].currentDataGrouping,q=c.ordinalIndex,h=p?p.count+p.unitName:"raw",a=c.options.overscroll,b=c.getExtremes(),
d;q||(q=c.ordinalIndex={});if(!q[h]){var e={series:[],chart:g,getExtremes:function(){return{min:b.dataMin,max:b.dataMax+a}},options:{ordinal:!0},val2lin:y.prototype.val2lin,ordinal2lin:y.prototype.ordinal2lin};c.series.forEach(function(a){d={xAxis:e,xData:a.xData.slice(),chart:g,destroyGroupedData:E};d.xData=d.xData.concat(c.getOverscrollPositions());d.options={dataGrouping:p?{enabled:!0,forced:!0,approximation:"open",units:[[p.unitName,[p.count]]]}:{enabled:!1}};a.processData.apply(d);e.series.push(d)});
c.beforeSetTickPositions.apply(e);q[h]=e.ordinalPositions}return q[h]},getOverscrollPositions:function(){var c=this.options.overscroll,g=this.overscrollPointsRange,p=[],q=this.dataMax;if(D(g))for(p.push(q);q<=this.dataMax+c;)q+=g,p.push(q);return p},getGroupIntervalFactor:function(c,g,p){p=p.processedXData;var m=p.length,h=[];var a=this.groupIntervalFactor;if(!a){for(a=0;a<m-1;a++)h[a]=p[a+1]-p[a];h.sort(function(a,c){return a-c});h=h[Math.floor(m/2)];c=Math.max(c,p[0]);g=Math.min(g,p[m-1]);this.groupIntervalFactor=
a=m*h/(g-c)}return a},postProcessTickInterval:function(c){var g=this.ordinalSlope;return g?this.options.breaks?this.closestPointRange||c:c/(g/this.closestPointRange):c}});y.prototype.ordinal2lin=y.prototype.val2lin;g(w,"pan",function(c){var g=this.xAxis[0],p=g.options.overscroll,q=c.originalEvent.chartX,h=!1;if(g.options.ordinal&&g.series.length){var a=this.mouseDownX,b=g.getExtremes(),d=b.dataMax,e=b.min,k=b.max,t=this.hoverPoints,z=g.closestPointRange||g.overscrollPointsRange;a=(a-q)/(g.translationSlope*
(g.ordinalSlope||z));var r={ordinalPositions:g.getExtendedPositions()};z=g.lin2val;var n=g.val2lin;if(!r.ordinalPositions)h=!0;else if(1<Math.abs(a)){t&&t.forEach(function(a){a.setState()});if(0>a){t=r;var f=g.ordinalPositions?g:r}else t=g.ordinalPositions?g:r,f=r;r=f.ordinalPositions;d>r[r.length-1]&&r.push(d);this.fixedRange=k-e;a=g.toFixedRange(null,null,z.apply(t,[n.apply(t,[e,!0])+a,!0]),z.apply(f,[n.apply(f,[k,!0])+a,!0]));a.min>=Math.min(b.dataMin,e)&&a.max<=Math.max(d,k)+p&&g.setExtremes(a.min,
a.max,!0,!1,{trigger:"pan"});this.mouseDownX=q;x(this.container,{cursor:"move"})}}else h=!0;h?p&&(g.max=g.dataMax+p):c.preventDefault()});g(y,"foundExtremes",function(){this.isXAxis&&D(this.options.overscroll)&&this.max===this.dataMax&&(!this.chart.mouseIsDown||this.isInternal)&&(!this.eventArgs||this.eventArgs&&"navigator"!==this.eventArgs.trigger)&&(this.max+=this.options.overscroll,!this.isInternal&&D(this.userMin)&&(this.min+=this.options.overscroll))});g(y,"afterSetScale",function(){this.horiz&&
!this.isDirty&&(this.isDirty=this.isOrdinal&&this.chart.navigator&&!this.chart.navigator.adaptToUpdatedData)})});K(D,"modules/broken-axis.src.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.extend,G=g.isArray,H=g.pick;g=c.addEvent;var y=c.find,w=c.fireEvent,x=c.Axis,E=c.Series,F=function(c,g){return y(g,function(g){return g.from<c&&c<g.to})};D(x.prototype,{isInBreak:function(c,g){var m=c.repeat||Infinity,q=c.from,h=c.to-c.from;g=g>=q?(g-q)%m:m-(q-g)%m;return c.inclusive?
g<=h:g<h&&0!==g},isInAnyBreak:function(c,g){var m=this.options.breaks,q=m&&m.length,h;if(q){for(;q--;)if(this.isInBreak(m[q],c)){var a=!0;h||(h=H(m[q].showPoints,!this.isXAxis))}var b=a&&g?a&&!h:a}return b}});g(x,"afterInit",function(){"function"===typeof this.setBreaks&&this.setBreaks(this.options.breaks,!1)});g(x,"afterSetTickPositions",function(){if(this.isBroken){var c=this.tickPositions,g=this.tickPositions.info,p=[],q;for(q=0;q<c.length;q++)this.isInAnyBreak(c[q])||p.push(c[q]);this.tickPositions=
p;this.tickPositions.info=g}});g(x,"afterSetOptions",function(){this.isBroken&&(this.options.ordinal=!1)});x.prototype.setBreaks=function(c,g){function m(a){var b=a,c;for(c=0;c<h.breakArray.length;c++){var g=h.breakArray[c];if(g.to<=a)b-=g.len;else if(g.from>=a)break;else if(h.isInBreak(g,a)){b-=a-g.from;break}}return b}function q(a){var b;for(b=0;b<h.breakArray.length;b++){var c=h.breakArray[b];if(c.from>=a)break;else c.to<a?a+=c.len:h.isInBreak(c,a)&&(a+=c.len)}return a}var h=this,a=G(c)&&!!c.length;
h.isDirty=h.isBroken!==a;h.isBroken=a;h.options.breaks=h.userOptions.breaks=c;h.forceRedraw=!0;a||h.val2lin!==m||(delete h.val2lin,delete h.lin2val);a&&(h.userOptions.ordinal=!1,h.val2lin=m,h.lin2val=q,h.setExtremes=function(a,c,e,g,h){if(this.isBroken){for(var b,d=this.options.breaks;b=F(a,d);)a=b.to;for(;b=F(c,d);)c=b.from;c<a&&(c=a)}x.prototype.setExtremes.call(this,a,c,e,g,h)},h.setAxisTranslation=function(a){x.prototype.setAxisTranslation.call(this,a);this.unitLength=null;if(this.isBroken){a=
h.options.breaks;var b=[],c=[],g=0,m,p=h.userMin||h.min,r=h.userMax||h.max,n=H(h.pointRangePadding,0),f;a.forEach(function(a){m=a.repeat||Infinity;h.isInBreak(a,p)&&(p+=a.to%m-p%m);h.isInBreak(a,r)&&(r-=r%m-a.from%m)});a.forEach(function(a){v=a.from;for(m=a.repeat||Infinity;v-m>p;)v-=m;for(;v<p;)v+=m;for(f=v;f<r;f+=m)b.push({value:f,move:"in"}),b.push({value:f+(a.to-a.from),move:"out",size:a.breakSize})});b.sort(function(a,b){return a.value===b.value?("in"===a.move?0:1)-("in"===b.move?0:1):a.value-
b.value});var l=0;var v=p;b.forEach(function(a){l+="in"===a.move?1:-1;1===l&&"in"===a.move&&(v=a.value);0===l&&(c.push({from:v,to:a.value,len:a.value-v-(a.size||0)}),g+=a.value-v-(a.size||0))});h.breakArray=c;h.unitLength=r-p-g+n;w(h,"afterBreaks");h.staticScale?h.transA=h.staticScale:h.unitLength&&(h.transA*=(r-h.min+n)/h.unitLength);n&&(h.minPixelPadding=h.transA*h.minPointOffset);h.min=p;h.max=r}});H(g,!0)&&this.chart.redraw()};g(E,"afterGeneratePoints",function(){var c=this.xAxis,g=this.yAxis,
p=this.points,q=p.length,h=this.options.connectNulls;if(c&&g&&(c.options.breaks||g.options.breaks))for(;q--;){var a=p[q];var b=null===a.y&&!1===h;b||!c.isInAnyBreak(a.x,!0)&&!g.isInAnyBreak(a.y,!0)||(p.splice(q,1),this.data[q]&&this.data[q].destroyElements())}});g(E,"afterRender",function(){this.drawBreaks(this.xAxis,["x"]);this.drawBreaks(this.yAxis,H(this.pointArrayMap,["y"]))});c.Series.prototype.drawBreaks=function(c,g){var m=this,q=m.points,h,a,b,d;c&&g.forEach(function(e){h=c.breakArray||[];
a=c.isXAxis?c.min:H(m.options.threshold,c.min);q.forEach(function(g){d=H(g["stack"+e.toUpperCase()],g[e]);h.forEach(function(e){b=!1;if(a<e.from&&d>e.to||a>e.from&&d<e.from)b="pointBreak";else if(a<e.from&&d>e.from&&d<e.to||a>e.from&&d>e.to&&d<e.from)b="pointInBreak";b&&w(c,b,{point:g,brk:e})})})})};c.Series.prototype.gappedPath=function(){var g=this.currentDataGrouping,m=g&&g.gapSize;g=this.options.gapSize;var p=this.points.slice(),q=p.length-1,h=this.yAxis;if(g&&0<q)for("value"!==this.options.gapUnit&&
(g*=this.basePointRange),m&&m>g&&m>=this.basePointRange&&(g=m);q--;)p[q+1].x-p[q].x>g&&(m=(p[q].x+p[q+1].x)/2,p.splice(q+1,0,{isNull:!0,x:m}),this.options.stacking&&(m=h.stacks[this.stackKey][m]=new c.StackItem(h,h.options.stackLabels,!1,m,this.stack),m.total=0));return this.getGraphPath(p)}});K(D,"masters/modules/broken-axis.src.js",[],function(){});K(D,"parts/DataGrouping.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.arrayMax,G=g.arrayMin,H=g.defined,y=g.extend,w=g.isNumber,
x=g.pick;g=c.addEvent;var E=c.Axis,F=c.correctFloat,t=c.defaultPlotOptions,m=c.format,p=c.merge,q=c.Point,h=c.Series,a=c.Tooltip,b=c.approximations={sum:function(a){var b=a.length;if(!b&&a.hasNulls)var c=null;else if(b)for(c=0;b--;)c+=a[b];return c},average:function(a){var c=a.length;a=b.sum(a);w(a)&&c&&(a=F(a/c));return a},averages:function(){var a=[];[].forEach.call(arguments,function(c){a.push(b.average(c))});return void 0===a[0]?void 0:a},open:function(a){return a.length?a[0]:a.hasNulls?null:
void 0},high:function(a){return a.length?D(a):a.hasNulls?null:void 0},low:function(a){return a.length?G(a):a.hasNulls?null:void 0},close:function(a){return a.length?a[a.length-1]:a.hasNulls?null:void 0},ohlc:function(a,c,d,f){a=b.open(a);c=b.high(c);d=b.low(d);f=b.close(f);if(w(a)||w(c)||w(d)||w(f))return[a,c,d,f]},range:function(a,c){a=b.low(a);c=b.high(c);if(w(a)||w(c))return[a,c];if(null===a&&null===c)return null}},d=function(a,c,d,f){var e=this,g=e.data,l=e.options&&e.options.data,k=[],h=[],m=
[],n=a.length,r=!!c,q=[],v=e.pointArrayMap,B=v&&v.length,z=["x"].concat(v||["y"]),t=0,A=0,C;f="function"===typeof f?f:b[f]?b[f]:b[e.getDGApproximation&&e.getDGApproximation()||"average"];B?v.forEach(function(){q.push([])}):q.push([]);var x=B||1;for(C=0;C<=n&&!(a[C]>=d[0]);C++);for(C;C<=n;C++){for(;void 0!==d[t+1]&&a[C]>=d[t+1]||C===n;){var y=d[t];e.dataGroupInfo={start:e.cropStart+A,length:q[0].length};var E=f.apply(e,q);e.pointClass&&!H(e.dataGroupInfo.options)&&(e.dataGroupInfo.options=p(e.pointClass.prototype.optionsToObject.call({series:e},
e.options.data[e.cropStart+A])),z.forEach(function(a){delete e.dataGroupInfo.options[a]}));void 0!==E&&(k.push(y),h.push(E),m.push(e.dataGroupInfo));A=C;for(y=0;y<x;y++)q[y].length=0,q[y].hasNulls=!1;t+=1;if(C===n)break}if(C===n)break;if(v)for(y=e.cropStart+C,E=g&&g[y]||e.pointClass.prototype.applyOptions.apply({series:e},[l[y]]),y=0;y<B;y++){var F=E[v[y]];w(F)?q[y].push(F):null===F&&(q[y].hasNulls=!0)}else y=r?c[C]:null,w(y)?q[0].push(y):null===y&&(q[0].hasNulls=!0)}return{groupedXData:k,groupedYData:h,
groupMap:m}},e={approximations:b,groupData:d},k=h.prototype,C=k.processData,z=k.generatePoints,r={groupPixelWidth:2,dateTimeLabelFormats:{millisecond:["%A, %b %e, %H:%M:%S.%L","%A, %b %e, %H:%M:%S.%L","-%H:%M:%S.%L"],second:["%A, %b %e, %H:%M:%S","%A, %b %e, %H:%M:%S","-%H:%M:%S"],minute:["%A, %b %e, %H:%M","%A, %b %e, %H:%M","-%H:%M"],hour:["%A, %b %e, %H:%M","%A, %b %e, %H:%M","-%H:%M"],day:["%A, %b %e, %Y","%A, %b %e","-%A, %b %e, %Y"],week:["Week from %A, %b %e, %Y","%A, %b %e","-%A, %b %e, %Y"],
month:["%B %Y","%B","-%B %Y"],year:["%Y","%Y","-%Y"]}},n={line:{},spline:{},area:{},areaspline:{},arearange:{},column:{groupPixelWidth:10},columnrange:{groupPixelWidth:10},candlestick:{groupPixelWidth:10},ohlc:{groupPixelWidth:5}},f=c.defaultDataGroupingUnits=[["millisecond",[1,2,5,10,20,25,50,100,200,500]],["second",[1,2,5,10,15,30]],["minute",[1,2,5,10,15,30]],["hour",[1,2,3,4,6,8,12]],["day",[1]],["week",[1]],["month",[1,3,6]],["year",null]];k.getDGApproximation=function(){return c.seriesTypes.arearange&&
this instanceof c.seriesTypes.arearange?"range":c.seriesTypes.ohlc&&this instanceof c.seriesTypes.ohlc?"ohlc":c.seriesTypes.column&&this instanceof c.seriesTypes.column?"sum":"average"};k.groupData=d;k.processData=function(){var a=this.chart,b=this.options.dataGrouping,c=!1!==this.allowDG&&b&&x(b.enabled,a.options.isStock),d=this.visible||!a.options.chart.ignoreHiddenSeries,e,g=this.currentDataGrouping,h=!1;this.forceCrop=c;this.groupPixelWidth=null;this.hasProcessed=!0;c&&!this.requireSorting&&(this.requireSorting=
h=!0);c=!1===C.apply(this,arguments)||!c;h&&(this.requireSorting=!1);if(!c){this.destroyGroupedData();c=b.groupAll?this.xData:this.processedXData;var m=b.groupAll?this.yData:this.processedYData,n=a.plotSizeX;a=this.xAxis;var r=a.options.ordinal,p=this.groupPixelWidth=a.getGroupPixelWidth&&a.getGroupPixelWidth();if(p){this.isDirty=e=!0;this.points=null;h=a.getExtremes();var q=h.min;h=h.max;r=r&&a.getGroupIntervalFactor(q,h,this)||1;p=p*(h-q)/n*r;n=a.getTimeTicks(a.normalizeTimeTickInterval(p,b.units||
f),Math.min(q,c[0]),Math.max(h,c[c.length-1]),a.options.startOfWeek,c,this.closestPointRange);m=k.groupData.apply(this,[c,m,n,b.approximation]);c=m.groupedXData;r=m.groupedYData;var z=0;if(b.smoothed&&c.length){var t=c.length-1;for(c[t]=Math.min(c[t],h);t--&&0<t;)c[t]+=p/2;c[0]=Math.max(c[0],q)}for(t=1;t<n.length;t++)n.info.segmentStarts&&-1!==n.info.segmentStarts.indexOf(t)||(z=Math.max(n[t]-n[t-1],z));q=n.info;q.gapSize=z;this.closestPointRange=n.info.totalRange;this.groupMap=m.groupMap;if(H(c[0])&&
c[0]<a.min&&d){if(!H(a.options.min)&&a.min<=a.dataMin||a.min===a.dataMin)a.min=Math.min(c[0],a.min);a.dataMin=c[0]}b.groupAll&&(b=this.cropData(c,r,a.min,a.max,1),c=b.xData,r=b.yData);this.processedXData=c;this.processedYData=r}else this.groupMap=null;this.hasGroupedData=e;this.currentDataGrouping=q;this.preventGraphAnimation=(g&&g.totalRange)!==(q&&q.totalRange)}};k.destroyGroupedData=function(){this.groupedData&&(this.groupedData.forEach(function(a,b){a&&(this.groupedData[b]=a.destroy?a.destroy():
null)},this),this.groupedData.length=0)};k.generatePoints=function(){z.apply(this);this.destroyGroupedData();this.groupedData=this.hasGroupedData?this.points:null};g(q,"update",function(){if(this.dataGroup)return c.error(24,!1,this.series.chart),!1});g(a,"headerFormatter",function(a){var b=this.chart.time,c=a.labelConfig,d=c.series,f=d.tooltipOptions,e=d.options.dataGrouping,g=f.xDateFormat,k=d.xAxis,l=f[(a.isFooter?"footer":"header")+"Format"];if(k&&"datetime"===k.options.type&&e&&w(c.key)){var h=
d.currentDataGrouping;e=e.dateTimeLabelFormats||r.dateTimeLabelFormats;if(h)if(f=e[h.unitName],1===h.count)g=f[0];else{g=f[1];var n=f[2]}else!g&&e&&(g=this.getXDateFormat(c,f,k));g=b.dateFormat(g,c.key);n&&(g+=b.dateFormat(n,c.key+h.totalRange-1));d.chart.styledMode&&(l=this.styledModeFormat(l));a.text=m(l,{point:y(c.point,{key:g}),series:d},b);a.preventDefault()}});g(h,"destroy",k.destroyGroupedData);g(h,"afterSetOptions",function(a){a=a.options;var b=this.type,c=this.chart.options.plotOptions,d=
t[b].dataGrouping,f=this.useCommonDataGrouping&&r;if(n[b]||f)d||(d=p(r,n[b])),a.dataGrouping=p(f,d,c.series&&c.series.dataGrouping,c[b].dataGrouping,this.userOptions.dataGrouping)});g(E,"afterSetScale",function(){this.series.forEach(function(a){a.hasProcessed=!1})});E.prototype.getGroupPixelWidth=function(){var a=this.series,b=a.length,c,d=0,f=!1,e;for(c=b;c--;)(e=a[c].options.dataGrouping)&&(d=Math.max(d,x(e.groupPixelWidth,r.groupPixelWidth)));for(c=b;c--;)(e=a[c].options.dataGrouping)&&a[c].hasProcessed&&
(b=(a[c].processedXData||a[c].data).length,a[c].groupPixelWidth||b>this.chart.plotSizeX/d||b&&e.forced)&&(f=!0);return f?d:0};E.prototype.setDataGrouping=function(a,b){var c;b=x(b,!0);a||(a={forced:!1,units:null});if(this instanceof E)for(c=this.series.length;c--;)this.series[c].update({dataGrouping:a},!1);else this.chart.options.series.forEach(function(b){b.dataGrouping=a},!1);this.ordinalSlope=null;b&&this.chart.redraw()};c.dataGrouping=e;"";return e});K(D,"parts/OHLCSeries.js",[D["parts/Globals.js"]],
function(c){var g=c.Point,D=c.seriesType,G=c.seriesTypes;D("ohlc","column",{lineWidth:1,tooltip:{pointFormat:'<span style="color:{point.color}">\u25cf</span> <b> {series.name}</b><br/>Open: {point.open}<br/>High: {point.high}<br/>Low: {point.low}<br/>Close: {point.close}<br/>'},threshold:null,states:{hover:{lineWidth:3}},stickyTracking:!0},{directTouch:!1,pointArrayMap:["open","high","low","close"],toYData:function(c){return[c.open,c.high,c.low,c.close]},pointValKey:"close",pointAttrToOptions:{stroke:"color",
"stroke-width":"lineWidth"},init:function(){G.column.prototype.init.apply(this,arguments);this.options.stacking=!1},pointAttribs:function(c,g){g=G.column.prototype.pointAttribs.call(this,c,g);var w=this.options;delete g.fill;!c.options.color&&w.upColor&&c.open<c.close&&(g.stroke=w.upColor);return g},translate:function(){var c=this,g=c.yAxis,w=!!c.modifyValue,x=["plotOpen","plotHigh","plotLow","plotClose","yBottom"];G.column.prototype.translate.apply(c);c.points.forEach(function(y){[y.open,y.high,
y.low,y.close,y.low].forEach(function(E,t){null!==E&&(w&&(E=c.modifyValue(E)),y[x[t]]=g.toPixels(E,!0))});y.tooltipPos[1]=y.plotHigh+g.pos-c.chart.plotTop})},drawPoints:function(){var c=this,g=c.chart;c.points.forEach(function(w){var x=w.graphic,y=!x;if(void 0!==w.plotY){x||(w.graphic=x=g.renderer.path().add(c.group));g.styledMode||x.attr(c.pointAttribs(w,w.selected&&"select"));var F=x.strokeWidth()%2/2;var t=Math.round(w.plotX)-F;var m=Math.round(w.shapeArgs.width/2);var p=["M",t,Math.round(w.yBottom),
"L",t,Math.round(w.plotHigh)];if(null!==w.open){var q=Math.round(w.plotOpen)+F;p.push("M",t,q,"L",t-m,q)}null!==w.close&&(q=Math.round(w.plotClose)+F,p.push("M",t,q,"L",t+m,q));x[y?"attr":"animate"]({d:p}).addClass(w.getClassName(),!0)}})},animate:null},{getClassName:function(){return g.prototype.getClassName.call(this)+(this.open<this.close?" highcharts-point-up":" highcharts-point-down")}});""});K(D,"parts/CandlestickSeries.js",[D["parts/Globals.js"]],function(c){var g=c.defaultPlotOptions,D=c.merge,
G=c.seriesType,H=c.seriesTypes;G("candlestick","ohlc",D(g.column,{states:{hover:{lineWidth:2}},tooltip:g.ohlc.tooltip,threshold:null,lineColor:"#000000",lineWidth:1,upColor:"#ffffff",stickyTracking:!0}),{pointAttribs:function(c,g){var w=H.column.prototype.pointAttribs.call(this,c,g),y=this.options,F=c.open<c.close,t=y.lineColor||this.color;w["stroke-width"]=y.lineWidth;w.fill=c.options.color||(F?y.upColor||this.color:this.color);w.stroke=c.options.lineColor||(F?y.upLineColor||t:t);g&&(c=y.states[g],
w.fill=c.color||w.fill,w.stroke=c.lineColor||w.stroke,w["stroke-width"]=c.lineWidth||w["stroke-width"]);return w},drawPoints:function(){var c=this,g=c.chart,x=c.yAxis.reversed;c.points.forEach(function(w){var y=w.graphic,t=!y;if(void 0!==w.plotY){y||(w.graphic=y=g.renderer.path().add(c.group));c.chart.styledMode||y.attr(c.pointAttribs(w,w.selected&&"select")).shadow(c.options.shadow);var m=y.strokeWidth()%2/2;var p=Math.round(w.plotX)-m;var q=w.plotOpen;var h=w.plotClose;var a=Math.min(q,h);q=Math.max(q,
h);var b=Math.round(w.shapeArgs.width/2);h=x?q!==w.yBottom:Math.round(a)!==Math.round(w.plotHigh);var d=x?Math.round(a)!==Math.round(w.plotHigh):q!==w.yBottom;a=Math.round(a)+m;q=Math.round(q)+m;m=[];m.push("M",p-b,q,"L",p-b,a,"L",p+b,a,"L",p+b,q,"Z","M",p,a,"L",p,h?Math.round(x?w.yBottom:w.plotHigh):a,"M",p,q,"L",p,d?Math.round(x?w.plotHigh:w.yBottom):q);y[t?"attr":"animate"]({d:m}).addClass(w.getClassName(),!0)}})}});""});K(D,"mixins/on-series.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],
function(c,g){var D=g.defined,G=c.seriesTypes,H=c.stableSort;return{getPlotBox:function(){return c.Series.prototype.getPlotBox.call(this.options.onSeries&&this.chart.get(this.options.onSeries)||this)},translate:function(){G.column.prototype.translate.apply(this);var c=this,g=c.options,x=c.chart,E=c.points,F=E.length-1,t,m=g.onSeries;m=m&&x.get(m);g=g.onKey||"y";var p=m&&m.options.step,q=m&&m.points,h=q&&q.length,a=x.inverted,b=c.xAxis,d=c.yAxis,e=0,k;if(m&&m.visible&&h){e=(m.pointXOffset||0)+(m.barW||
0)/2;x=m.currentDataGrouping;var C=q[h-1].x+(x?x.totalRange:0);H(E,function(a,b){return a.x-b.x});for(g="plot"+g[0].toUpperCase()+g.substr(1);h--&&E[F];){var z=q[h];x=E[F];x.y=z.y;if(z.x<=x.x&&void 0!==z[g]){if(x.x<=C&&(x.plotY=z[g],z.x<x.x&&!p&&(k=q[h+1])&&void 0!==k[g])){var r=(x.x-z.x)/(k.x-z.x);x.plotY+=r*(k[g]-z[g]);x.y+=r*(k.y-z.y)}F--;h++;if(0>F)break}}}E.forEach(function(g,f){g.plotX+=e;if(void 0===g.plotY||a)0<=g.plotX&&g.plotX<=b.len?a?(g.plotY=b.translate(g.x,0,1,0,1),g.plotX=D(g.y)?d.translate(g.y,
0,0,0,1):0):g.plotY=(b.opposite?0:c.yAxis.len)+b.offset:g.shapeArgs={};if((t=E[f-1])&&t.plotX===g.plotX){void 0===t.stackIndex&&(t.stackIndex=0);var k=t.stackIndex+1}g.stackIndex=k});this.onSeries=m}}});K(D,"parts/FlagsSeries.js",[D["parts/Globals.js"],D["parts/Utilities.js"],D["mixins/on-series.js"]],function(c,g,D){function G(a){h[a+"pin"]=function(b,c,e,g,m){var d=m&&m.anchorX;m=m&&m.anchorY;"circle"===a&&g>e&&(b-=Math.round((g-e)/2),e=g);var k=h[a](b,c,e,g);d&&m&&(k.push("M","circle"===a?b+e/
2:k[1]+k[4]/2,c>m?c:c+g,"L",d,m),k=k.concat(h.circle(d-1,m-1,2,2)));return k}}var H=g.defined,y=g.isNumber,w=g.objectEach,x=c.addEvent,E=c.merge;g=c.noop;var F=c.Renderer,t=c.Series,m=c.seriesType,p=c.TrackerMixin,q=c.VMLRenderer,h=c.SVGRenderer.prototype.symbols;m("flags","column",{pointRange:0,allowOverlapX:!1,shape:"flag",stackDistance:12,textAlign:"center",tooltip:{pointFormat:"{point.text}<br/>"},threshold:null,y:-30,fillColor:"#ffffff",lineWidth:1,states:{hover:{lineColor:"#000000",fillColor:"#ccd6eb"}},
style:{fontSize:"11px",fontWeight:"bold"}},{sorted:!1,noSharedTooltip:!0,allowDG:!1,takeOrdinalPosition:!1,trackerGroups:["markerGroup"],forceCrop:!0,init:t.prototype.init,pointAttribs:function(a,b){var c=this.options,e=a&&a.color||this.color,g=c.lineColor,h=a&&a.lineWidth;a=a&&a.fillColor||c.fillColor;b&&(a=c.states[b].fillColor,g=c.states[b].lineColor,h=c.states[b].lineWidth);return{fill:a||e,stroke:g||e,"stroke-width":h||c.lineWidth||0}},translate:D.translate,getPlotBox:D.getPlotBox,drawPoints:function(){var a=
this.points,b=this.chart,d=b.renderer,e=b.inverted,g=this.options,h=g.y,m,r=this.yAxis,n={},f=[];for(m=a.length;m--;){var l=a[m];var p=(e?l.plotY:l.plotX)>this.xAxis.len;var q=l.plotX;var t=l.stackIndex;var u=l.options.shape||g.shape;var x=l.plotY;void 0!==x&&(x=l.plotY+h-(void 0!==t&&t*g.stackDistance));l.anchorX=t?void 0:l.plotX;var y=t?void 0:l.plotY;var F="flag"!==u;t=l.graphic;void 0!==x&&0<=q&&!p?(t||(t=l.graphic=d.label("",null,null,u,null,null,g.useHTML),b.styledMode||t.attr(this.pointAttribs(l)).css(E(g.style,
l.style)),t.attr({align:F?"center":"left",width:g.width,height:g.height,"text-align":g.textAlign}).addClass("highcharts-point").add(this.markerGroup),l.graphic.div&&(l.graphic.div.point=l),b.styledMode||t.shadow(g.shadow),t.isNew=!0),0<q&&(q-=t.strokeWidth()%2),u={y:x,anchorY:y},g.allowOverlapX&&(u.x=q,u.anchorX=l.anchorX),t.attr({text:l.options.title||g.title||"A"})[t.isNew?"attr":"animate"](u),g.allowOverlapX||(n[l.plotX]?n[l.plotX].size=Math.max(n[l.plotX].size,t.width):n[l.plotX]={align:F?.5:
0,size:t.width,target:q,anchorX:q}),l.tooltipPos=[q,x+r.pos-b.plotTop]):t&&(l.graphic=t.destroy())}g.allowOverlapX||(w(n,function(a){a.plotX=a.anchorX;f.push(a)}),c.distribute(f,e?r.len:this.xAxis.len,100),a.forEach(function(a){var b=a.graphic&&n[a.plotX];b&&(a.graphic[a.graphic.isNew?"attr":"animate"]({x:b.pos+b.align*b.size,anchorX:a.anchorX}),H(b.pos)?a.graphic.isNew=!1:(a.graphic.attr({x:-9999,anchorX:-9999}),a.graphic.isNew=!0))}));g.useHTML&&c.wrap(this.markerGroup,"on",function(a){return c.SVGElement.prototype.on.apply(a.apply(this,
[].slice.call(arguments,1)),[].slice.call(arguments,1))})},drawTracker:function(){var a=this.points;p.drawTrackerPoint.apply(this);a.forEach(function(b){var c=b.graphic;c&&x(c.element,"mouseover",function(){0<b.stackIndex&&!b.raised&&(b._y=c.y,c.attr({y:b._y-8}),b.raised=!0);a.forEach(function(a){a!==b&&a.raised&&a.graphic&&(a.graphic.attr({y:a._y}),a.raised=!1)})})})},animate:function(a){a?this.setClip():this.animate=null},setClip:function(){t.prototype.setClip.apply(this,arguments);!1!==this.options.clip&&
this.sharedClipKey&&this.markerGroup.clip(this.chart[this.sharedClipKey])},buildKDTree:g,invertGroups:g},{isValid:function(){return y(this.y)||void 0===this.y}});h.flag=function(a,b,c,e,g){var d=g&&g.anchorX||a;g=g&&g.anchorY||b;return h.circle(d-1,g-1,2,2).concat(["M",d,g,"L",a,b+e,a,b,a+c,b,a+c,b+e,a,b+e,"Z"])};G("circle");G("square");F===q&&["circlepin","flag","squarepin"].forEach(function(a){q.prototype.symbols[a]=h[a]});""});K(D,"parts/RangeSelector.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],
function(c,g){function D(a){this.init(a)}var G=g.defined,H=g.destroyObjectProperties,y=g.discardElement,w=g.extend,x=g.isNumber,E=g.objectEach,F=g.pick,t=g.pInt,m=g.splat,p=c.addEvent,q=c.Axis;g=c.Chart;var h=c.css,a=c.createElement,b=c.defaultOptions,d=c.fireEvent,e=c.merge;w(b,{rangeSelector:{verticalAlign:"top",buttonTheme:{width:28,height:18,padding:2,zIndex:7},floating:!1,x:0,y:0,height:void 0,inputPosition:{align:"right",x:0,y:0},buttonPosition:{align:"left",x:0,y:0},labelStyle:{color:"#666666"}}});
b.lang=e(b.lang,{rangeSelectorZoom:"Zoom",rangeSelectorFrom:"From",rangeSelectorTo:"To"});D.prototype={clickButton:function(a,b){var c=this.chart,d=this.buttonOptions[a],e=c.xAxis[0],f=c.scroller&&c.scroller.getUnionExtremes()||e||{},g=f.dataMin,k=f.dataMax,h=e&&Math.round(Math.min(e.max,F(k,e.max))),t=d.type;f=d._range;var u,w=d.dataGrouping;if(null!==g&&null!==k){c.fixedRange=f;w&&(this.forcedDataGrouping=!0,q.prototype.setDataGrouping.call(e||{chart:this.chart},w,!1),this.frozenStates=d.preserveDataGrouping);
if("month"===t||"year"===t)if(e){t={range:d,max:h,chart:c,dataMin:g,dataMax:k};var C=e.minFromRange.call(t);x(t.newMax)&&(h=t.newMax)}else f=d;else if(f)C=Math.max(h-f,g),h=Math.min(C+f,k);else if("ytd"===t)if(e)void 0===k&&(g=Number.MAX_VALUE,k=Number.MIN_VALUE,c.series.forEach(function(a){a=a.xData;g=Math.min(a[0],g);k=Math.max(a[a.length-1],k)}),b=!1),h=this.getYTDExtremes(k,g,c.time.useUTC),C=u=h.min,h=h.max;else{this.deferredYTDClick=a;return}else"all"===t&&e&&(C=g,h=k);C+=d._offsetMin;h+=d._offsetMax;
this.setSelected(a);if(e)e.setExtremes(C,h,F(b,1),null,{trigger:"rangeSelectorButton",rangeSelectorButton:d});else{var y=m(c.options.xAxis)[0];var E=y.range;y.range=f;var D=y.min;y.min=u;p(c,"load",function(){y.range=E;y.min=D})}}},setSelected:function(a){this.selected=this.options.selected=a},defaultButtons:[{type:"month",count:1,text:"1m"},{type:"month",count:3,text:"3m"},{type:"month",count:6,text:"6m"},{type:"ytd",text:"YTD"},{type:"year",count:1,text:"1y"},{type:"all",text:"All"}],init:function(a){var b=
this,c=a.options.rangeSelector,e=c.buttons||[].concat(b.defaultButtons),g=c.selected,f=function(){var a=b.minInput,c=b.maxInput;a&&a.blur&&d(a,"blur");c&&c.blur&&d(c,"blur")};b.chart=a;b.options=c;b.buttons=[];b.buttonOptions=e;this.unMouseDown=p(a.container,"mousedown",f);this.unResize=p(a,"resize",f);e.forEach(b.computeButtonRange);void 0!==g&&e[g]&&this.clickButton(g,!1);p(a,"load",function(){a.xAxis&&a.xAxis[0]&&p(a.xAxis[0],"setExtremes",function(c){this.max-this.min!==a.fixedRange&&"rangeSelectorButton"!==
c.trigger&&"updatedData"!==c.trigger&&b.forcedDataGrouping&&!b.frozenStates&&this.setDataGrouping(!1,!1)})})},updateButtonStates:function(){var a=this,b=this.chart,c=b.xAxis[0],d=Math.round(c.max-c.min),e=!c.hasVisibleSeries,f=b.scroller&&b.scroller.getUnionExtremes()||c,g=f.dataMin,h=f.dataMax;b=a.getYTDExtremes(h,g,b.time.useUTC);var m=b.min,p=b.max,q=a.selected,t=x(q),w=a.options.allButtonsEnabled,y=a.buttons;a.buttonOptions.forEach(function(b,f){var k=b._range,l=b.type,n=b.count||1,r=y[f],v=0,
u=b._offsetMax-b._offsetMin;b=f===q;var z=k>h-g,B=k<c.minRange,x=!1,C=!1;k=k===d;("month"===l||"year"===l)&&d+36E5>=864E5*{month:28,year:365}[l]*n-u&&d-36E5<=864E5*{month:31,year:366}[l]*n+u?k=!0:"ytd"===l?(k=p-m+u===d,x=!b):"all"===l&&(k=c.max-c.min>=h-g,C=!b&&t&&k);l=!w&&(z||B||C||e);n=b&&k||k&&!t&&!x||b&&a.frozenStates;l?v=3:n&&(t=!0,v=2);r.state!==v&&(r.setState(v),0===v&&q===f&&a.setSelected(null))})},computeButtonRange:function(a){var b=a.type,c=a.count||1,d={millisecond:1,second:1E3,minute:6E4,
hour:36E5,day:864E5,week:6048E5};if(d[b])a._range=d[b]*c;else if("month"===b||"year"===b)a._range=864E5*{month:30,year:365}[b]*c;a._offsetMin=F(a.offsetMin,0);a._offsetMax=F(a.offsetMax,0);a._range+=a._offsetMax-a._offsetMin},setInputValue:function(a,b){var c=this.chart.options.rangeSelector,d=this.chart.time,e=this[a+"Input"];G(b)&&(e.previousValue=e.HCTime,e.HCTime=b);e.value=d.dateFormat(c.inputEditDateFormat||"%Y-%m-%d",e.HCTime);this[a+"DateBox"].attr({text:d.dateFormat(c.inputDateFormat||"%b %e, %Y",
e.HCTime)})},showInput:function(a){var b=this.inputGroup,c=this[a+"DateBox"];h(this[a+"Input"],{left:b.translateX+c.x+"px",top:b.translateY+"px",width:c.width-2+"px",height:c.height-2+"px",border:"2px solid silver"})},hideInput:function(a){h(this[a+"Input"],{border:0,width:"1px",height:"1px"});this.setInputValue(a)},drawInput:function(d){function g(){var a=A.value,b=(l.inputDateParser||Date.parse)(a),c=m.xAxis[0],d=m.scroller&&m.scroller.xAxis?m.scroller.xAxis:c,f=d.dataMin;d=d.dataMax;b!==A.previousValue&&
(A.previousValue=b,x(b)||(b=a.split("-"),b=Date.UTC(t(b[0]),t(b[1])-1,t(b[2]))),x(b)&&(m.time.useUTC||(b+=6E4*(new Date).getTimezoneOffset()),q?b>k.maxInput.HCTime?b=void 0:b<f&&(b=f):b<k.minInput.HCTime?b=void 0:b>d&&(b=d),void 0!==b&&c.setExtremes(q?b:c.min,q?c.max:b,void 0,void 0,{trigger:"rangeSelectorInput"})))}var k=this,m=k.chart,n=m.renderer.style||{},f=m.renderer,l=m.options.rangeSelector,p=k.div,q="min"===d,A,u,y=this.inputGroup;this[d+"Label"]=u=f.label(b.lang[q?"rangeSelectorFrom":"rangeSelectorTo"],
this.inputGroup.offset).addClass("highcharts-range-label").attr({padding:2}).add(y);y.offset+=u.width+5;this[d+"DateBox"]=f=f.label("",y.offset).addClass("highcharts-range-input").attr({padding:2,width:l.inputBoxWidth||90,height:l.inputBoxHeight||17,"text-align":"center"}).on("click",function(){k.showInput(d);k[d+"Input"].focus()});m.styledMode||f.attr({stroke:l.inputBoxBorderColor||"#cccccc","stroke-width":1});f.add(y);y.offset+=f.width+(q?10:0);this[d+"Input"]=A=a("input",{name:d,className:"highcharts-range-selector",
type:"text"},{top:m.plotTop+"px"},p);m.styledMode||(u.css(e(n,l.labelStyle)),f.css(e({color:"#333333"},n,l.inputStyle)),h(A,w({position:"absolute",border:0,width:"1px",height:"1px",padding:0,textAlign:"center",fontSize:n.fontSize,fontFamily:n.fontFamily,top:"-9999em"},l.inputStyle)));A.onfocus=function(){k.showInput(d)};A.onblur=function(){A===c.doc.activeElement&&g();k.hideInput(d);A.blur()};A.onchange=g;A.onkeypress=function(a){13===a.keyCode&&g()}},getPosition:function(){var a=this.chart,b=a.options.rangeSelector;
a="top"===b.verticalAlign?a.plotTop-a.axisOffset[0]:0;return{buttonTop:a+b.buttonPosition.y,inputTop:a+b.inputPosition.y-10}},getYTDExtremes:function(a,b,c){var d=this.chart.time,e=new d.Date(a),f=d.get("FullYear",e);c=c?d.Date.UTC(f,0,1):+new d.Date(f,0,1);b=Math.max(b||0,c);e=e.getTime();return{max:Math.min(a||e,e),min:b}},render:function(c,d){var e=this,g=e.chart,k=g.renderer,f=g.container,l=g.options,h=l.exporting&&!1!==l.exporting.enabled&&l.navigation&&l.navigation.buttonOptions,m=b.lang,p=
e.div,q=l.rangeSelector,t=F(l.chart.style&&l.chart.style.zIndex,0)+1;l=q.floating;var w=e.buttons;p=e.inputGroup;var x=q.buttonTheme,y=q.buttonPosition,C=q.inputPosition,E=q.inputEnabled,D=x&&x.states,G=g.plotLeft,H=e.buttonGroup;var I=e.rendered;var K=e.options.verticalAlign,X=g.legend,R=X&&X.options,T=y.y,ba=C.y,Z=I||!1,aa=Z?"animate":"attr",V=0,W=0,Y;if(!1!==q.enabled){I||(e.group=I=k.g("range-selector-group").attr({zIndex:7}).add(),e.buttonGroup=H=k.g("range-selector-buttons").add(I),e.zoomText=
k.text(m.rangeSelectorZoom,0,15).add(H),g.styledMode||(e.zoomText.css(q.labelStyle),x["stroke-width"]=F(x["stroke-width"],0)),e.buttonOptions.forEach(function(a,b){w[b]=k.button(a.text,0,0,function(c){var d=a.events&&a.events.click,f;d&&(f=d.call(a,c));!1!==f&&e.clickButton(b);e.isActive=!0},x,D&&D.hover,D&&D.select,D&&D.disabled).attr({"text-align":"center"}).add(H)}),!1!==E&&(e.div=p=a("div",null,{position:"relative",height:0,zIndex:t}),f.parentNode.insertBefore(p,f),e.inputGroup=p=k.g("input-group").add(I),
p.offset=0,e.drawInput("min"),e.drawInput("max")));e.zoomText[aa]({x:F(G+y.x,G)});var ea=F(G+y.x,G)+e.zoomText.getBBox().width+5;e.buttonOptions.forEach(function(a,b){w[b][aa]({x:ea});ea+=w[b].width+F(q.buttonSpacing,5)});G=g.plotLeft-g.spacing[3];e.updateButtonStates();h&&this.titleCollision(g)&&"top"===K&&"right"===y.align&&y.y+H.getBBox().height-12<(h.y||0)+h.height&&(V=-40);"left"===y.align?Y=y.x-g.spacing[3]:"right"===y.align&&(Y=y.x+V-g.spacing[1]);H.align({y:y.y,width:H.getBBox().width,align:y.align,
x:Y},!0,g.spacingBox);e.group.placed=Z;e.buttonGroup.placed=Z;!1!==E&&(V=h&&this.titleCollision(g)&&"top"===K&&"right"===C.align&&C.y-p.getBBox().height-12<(h.y||0)+h.height+g.spacing[0]?-40:0,"left"===C.align?Y=G:"right"===C.align&&(Y=-Math.max(g.axisOffset[1],-V)),p.align({y:C.y,width:p.getBBox().width,align:C.align,x:C.x+Y-2},!0,g.spacingBox),f=p.alignAttr.translateX+p.alignOptions.x-V+p.getBBox().x+2,h=p.alignOptions.width,m=H.alignAttr.translateX+H.getBBox().x,Y=H.getBBox().width+20,(C.align===
y.align||m+Y>f&&f+h>m&&T<ba+p.getBBox().height)&&p.attr({translateX:p.alignAttr.translateX+(g.axisOffset[1]>=-V?0:-V),translateY:p.alignAttr.translateY+H.getBBox().height+10}),e.setInputValue("min",c),e.setInputValue("max",d),e.inputGroup.placed=Z);e.group.align({verticalAlign:K},!0,g.spacingBox);c=e.group.getBBox().height+20;d=e.group.alignAttr.translateY;"bottom"===K&&(X=R&&"bottom"===R.verticalAlign&&R.enabled&&!R.floating?X.legendHeight+F(R.margin,10):0,c=c+X-20,W=d-c-(l?0:q.y)-(g.titleOffset?
g.titleOffset[2]:0)-10);if("top"===K)l&&(W=0),g.titleOffset&&g.titleOffset[0]&&(W=g.titleOffset[0]),W+=g.margin[0]-g.spacing[0]||0;else if("middle"===K)if(ba===T)W=0>ba?d+void 0:d;else if(ba||T)W=0>ba||0>T?W-Math.min(ba,T):d-c+NaN;e.group.translate(q.x,q.y+Math.floor(W));!1!==E&&(e.minInput.style.marginTop=e.group.translateY+"px",e.maxInput.style.marginTop=e.group.translateY+"px");e.rendered=!0}},getHeight:function(){var a=this.options,b=this.group,c=a.y,d=a.buttonPosition.y,e=a.inputPosition.y;if(a.height)return a.height;
a=b?b.getBBox(!0).height+13+c:0;b=Math.min(e,d);if(0>e&&0>d||0<e&&0<d)a+=Math.abs(b);return a},titleCollision:function(a){return!(a.options.title.text||a.options.subtitle.text)},update:function(a){var b=this.chart;e(!0,b.options.rangeSelector,a);this.destroy();this.init(b);b.rangeSelector.render()},destroy:function(){var a=this,b=a.minInput,c=a.maxInput;a.unMouseDown();a.unResize();H(a.buttons);b&&(b.onfocus=b.onblur=b.onchange=null);c&&(c.onfocus=c.onblur=c.onchange=null);E(a,function(b,c){b&&"chart"!==
c&&(b.destroy?b.destroy():b.nodeType&&y(this[c]));b!==D.prototype[c]&&(a[c]=null)},this)}};q.prototype.minFromRange=function(){var a=this.range,b={month:"Month",year:"FullYear"}[a.type],c=this.max,d=this.chart.time,e=function(a,c){var f=new d.Date(a),e=d.get(b,f);d.set(b,f,e+c);e===d.get(b,f)&&d.set("Date",f,0);return f.getTime()-a};if(x(a)){var f=c-a;var g=a}else f=c+e(c,-a.count),this.chart&&(this.chart.fixedRange=c-f);var h=F(this.dataMin,Number.MIN_VALUE);x(f)||(f=h);f<=h&&(f=h,void 0===g&&(g=
e(f,a.count)),this.newMax=Math.min(f+g,this.dataMax));x(c)||(f=void 0);return f};c.RangeSelector||(p(g,"afterGetContainer",function(){this.options.rangeSelector.enabled&&(this.rangeSelector=new D(this))}),p(g,"beforeRender",function(){var a=this.axes,b=this.rangeSelector;b&&(x(b.deferredYTDClick)&&(b.clickButton(b.deferredYTDClick),delete b.deferredYTDClick),a.forEach(function(a){a.updateNames();a.setScale()}),this.getAxisMargins(),b.render(),a=b.options.verticalAlign,b.options.floating||("bottom"===
a?this.extraBottomMargin=!0:"middle"!==a&&(this.extraTopMargin=!0)))}),p(g,"update",function(a){var b=a.options.rangeSelector;a=this.rangeSelector;var c=this.extraBottomMargin,d=this.extraTopMargin;b&&b.enabled&&!G(a)&&(this.options.rangeSelector.enabled=!0,this.rangeSelector=new D(this));this.extraTopMargin=this.extraBottomMargin=!1;a&&(a.render(),b=b&&b.verticalAlign||a.options&&a.options.verticalAlign,a.options.floating||("bottom"===b?this.extraBottomMargin=!0:"middle"!==b&&(this.extraTopMargin=
!0)),this.extraBottomMargin!==c||this.extraTopMargin!==d)&&(this.isDirtyBox=!0)}),p(g,"render",function(){var a=this.rangeSelector;a&&!a.options.floating&&(a.render(),a=a.options.verticalAlign,"bottom"===a?this.extraBottomMargin=!0:"middle"!==a&&(this.extraTopMargin=!0))}),p(g,"getMargins",function(){var a=this.rangeSelector;a&&(a=a.getHeight(),this.extraTopMargin&&(this.plotTop+=a),this.extraBottomMargin&&(this.marginBottom+=a))}),g.prototype.callbacks.push(function(a){function b(){c=a.xAxis[0].getExtremes();
x(c.min)&&d.render(c.min,c.max)}var c,d=a.rangeSelector;if(d){var e=p(a.xAxis[0],"afterSetExtremes",function(a){d.render(a.min,a.max)});var f=p(a,"redraw",b);b()}p(a,"destroy",function(){d&&(f(),e())})}),c.RangeSelector=D)});K(D,"parts/StockChart.js",[D["parts/Globals.js"],D["parts/Utilities.js"]],function(c,g){var D=g.arrayMax,G=g.arrayMin,H=g.defined,y=g.extend,w=g.isNumber,x=g.isString,E=g.pick,F=g.splat;g=c.addEvent;var t=c.Axis,m=c.Chart,p=c.format,q=c.merge,h=c.Point,a=c.Renderer,b=c.Series,
d=c.SVGRenderer,e=c.VMLRenderer,k=b.prototype,C=k.init,z=k.processData,r=h.prototype.tooltipFormatter;c.StockChart=c.stockChart=function(a,b,d){var f=x(a)||a.nodeName,e=arguments[f?1:0],g=e,l=e.series,h=c.getOptions(),k,n=E(e.navigator&&e.navigator.enabled,h.navigator.enabled,!0),p=n?{startOnTick:!1,endOnTick:!1}:null;e.xAxis=F(e.xAxis||{}).map(function(a,b){return q({minPadding:0,maxPadding:0,overscroll:0,ordinal:!0,title:{text:null},labels:{overflow:"justify"},showLastLabel:!0},h.xAxis,h.xAxis&&
h.xAxis[b],a,{type:"datetime",categories:null},p)});e.yAxis=F(e.yAxis||{}).map(function(a,b){k=E(a.opposite,!0);return q({labels:{y:-2},opposite:k,showLastLabel:!(!a.categories&&"category"!==a.type),title:{text:null}},h.yAxis,h.yAxis&&h.yAxis[b],a)});e.series=null;e=q({chart:{panning:!0,pinchType:"x"},navigator:{enabled:n},scrollbar:{enabled:E(h.scrollbar.enabled,!0)},rangeSelector:{enabled:E(h.rangeSelector.enabled,!0)},title:{text:null},tooltip:{split:E(h.tooltip.split,!0),crosshairs:!0},legend:{enabled:!1}},
e,{isStock:!0});e.series=g.series=l;return f?new m(a,e,d):new m(e,b)};g(b,"setOptions",function(a){function b(a){return c.seriesTypes[a]&&d instanceof c.seriesTypes[a]}var d=this,e;this.chart.options.isStock&&(b("column")||b("columnrange")?e={borderWidth:0,shadow:!1}:!b("line")||b("scatter")||b("sma")||(e={marker:{enabled:!1,radius:2}}),e&&(a.plotOptions[this.type]=q(a.plotOptions[this.type],e)))});g(t,"autoLabelAlign",function(a){var b=this.chart,c=this.options;b=b._labelPanes=b._labelPanes||{};
var d=this.options.labels;this.chart.options.isStock&&"yAxis"===this.coll&&(c=c.top+","+c.height,!b[c]&&d.enabled&&(15===d.x&&(d.x=0),void 0===d.align&&(d.align="right"),b[c]=this,a.align="right",a.preventDefault()))});g(t,"destroy",function(){var a=this.chart,b=this.options&&this.options.top+","+this.options.height;b&&a._labelPanes&&a._labelPanes[b]===this&&delete a._labelPanes[b]});g(t,"getPlotLinePath",function(a){function b(a){var b="xAxis"===a?"yAxis":"xAxis";a=d.options[b];return w(a)?[g[b][a]]:
x(a)?[g.get(a)]:e.map(function(a){return a[b]})}var d=this,e=this.isLinked&&!this.series?this.linkedParent.series:this.series,g=d.chart,h=g.renderer,k=d.left,m=d.top,n,p,q,r,t=[],y=[],z=a.translatedValue,C=a.value,D=a.force;if(g.options.isStock&&!1!==a.acrossPanes&&"xAxis"===d.coll||"yAxis"===d.coll){a.preventDefault();y=b(d.coll);var F=d.isXAxis?g.yAxis:g.xAxis;F.forEach(function(a){if(H(a.options.id)?-1===a.options.id.indexOf("navigator"):1){var b=a.isXAxis?"yAxis":"xAxis";b=H(a.options[b])?g[b][a.options[b]]:
g[b][0];d===b&&y.push(a)}});var G=y.length?[]:[d.isXAxis?g.yAxis[0]:g.xAxis[0]];y.forEach(function(a){-1!==G.indexOf(a)||c.find(G,function(b){return b.pos===a.pos&&b.len===a.len})||G.push(a)});var I=E(z,d.translate(C,null,null,a.old));w(I)&&(d.horiz?G.forEach(function(a){var b;p=a.pos;r=p+a.len;n=q=Math.round(I+d.transB);"pass"!==D&&(n<k||n>k+d.width)&&(D?n=q=Math.min(Math.max(k,n),k+d.width):b=!0);b||t.push("M",n,p,"L",q,r)}):G.forEach(function(a){var b;n=a.pos;q=n+a.len;p=r=Math.round(m+d.height-
I);"pass"!==D&&(p<m||p>m+d.height)&&(D?p=r=Math.min(Math.max(m,p),d.top+d.height):b=!0);b||t.push("M",n,p,"L",q,r)}));a.path=0<t.length?h.crispPolyLine(t,a.lineWidth||1):null}});d.prototype.crispPolyLine=function(a,b){var c;for(c=0;c<a.length;c+=6)a[c+1]===a[c+4]&&(a[c+1]=a[c+4]=Math.round(a[c+1])-b%2/2),a[c+2]===a[c+5]&&(a[c+2]=a[c+5]=Math.round(a[c+2])+b%2/2);return a};a===e&&(e.prototype.crispPolyLine=d.prototype.crispPolyLine);g(t,"afterHideCrosshair",function(){this.crossLabel&&(this.crossLabel=
this.crossLabel.hide())});g(t,"afterDrawCrosshair",function(a){var b,c;if(H(this.crosshair.label)&&this.crosshair.label.enabled&&this.cross){var d=this.chart,e=this.options.crosshair.label,g=this.horiz,h=this.opposite,k=this.left,m=this.top,n=this.crossLabel,q=e.format,r="",t="inside"===this.options.tickPosition,w=!1!==this.crosshair.snap,x=0,z=a.e||this.cross&&this.cross.e,C=a.point;var D=this.lin2log;if(this.isLog){a=D(this.min);var F=D(this.max)}else a=this.min,F=this.max;D=g?"center":h?"right"===
this.labelAlign?"right":"left":"left"===this.labelAlign?"left":"center";n||(n=this.crossLabel=d.renderer.label(null,null,null,e.shape||"callout").addClass("highcharts-crosshair-label"+(this.series[0]&&" highcharts-color-"+this.series[0].colorIndex)).attr({align:e.align||D,padding:E(e.padding,8),r:E(e.borderRadius,3),zIndex:2}).add(this.labelGroup),d.styledMode||n.attr({fill:e.backgroundColor||this.series[0]&&this.series[0].color||"#666666",stroke:e.borderColor||"","stroke-width":e.borderWidth||0}).css(y({color:"#ffffff",
fontWeight:"normal",fontSize:"11px",textAlign:"center"},e.style)));g?(D=w?C.plotX+k:z.chartX,m+=h?0:this.height):(D=h?this.width+k:0,m=w?C.plotY+m:z.chartY);q||e.formatter||(this.isDatetimeAxis&&(r="%b %d, %Y"),q="{value"+(r?":"+r:"")+"}");r=w?C[this.isXAxis?"x":"y"]:this.toValue(g?z.chartX:z.chartY);n.attr({text:q?p(q,{value:r},d.time):e.formatter.call(this,r),x:D,y:m,visibility:r<a||r>F?"hidden":"visible"});e=n.getBBox();if(g){if(t&&!h||!t&&h)m=n.y-e.height}else m=n.y-e.height/2;g?(b=k-e.x,c=k+
this.width-e.x):(b="left"===this.labelAlign?k:0,c="right"===this.labelAlign?k+this.width:d.chartWidth);n.translateX<b&&(x=b-n.translateX);n.translateX+e.width>=c&&(x=-(n.translateX+e.width-c));n.attr({x:D+x,y:m,anchorX:g?D:this.opposite?0:d.chartWidth,anchorY:g?this.opposite?d.chartHeight:0:m+e.height/2})}});k.init=function(){C.apply(this,arguments);this.setCompare(this.options.compare)};k.setCompare=function(a){this.modifyValue="value"===a||"percent"===a?function(b,c){var d=this.compareValue;return void 0!==
b&&void 0!==d?(b="value"===a?b-d:b/d*100-(100===this.options.compareBase?0:100),c&&(c.change=b),b):0}:null;this.userOptions.compare=a;this.chart.hasRendered&&(this.isDirty=!0)};k.processData=function(a){var b,c=-1,d=!0===this.options.compareStart?0:1;z.apply(this,arguments);if(this.xAxis&&this.processedYData){var e=this.processedXData;var g=this.processedYData;var h=g.length;this.pointArrayMap&&(c=this.pointArrayMap.indexOf(this.options.pointValKey||this.pointValKey||"y"));for(b=0;b<h-d;b++){var k=
g[b]&&-1<c?g[b][c]:g[b];if(w(k)&&e[b+d]>=this.xAxis.min&&0!==k){this.compareValue=k;break}}}};g(b,"afterGetExtremes",function(){if(this.modifyValue){var a=[this.modifyValue(this.dataMin),this.modifyValue(this.dataMax)];this.dataMin=G(a);this.dataMax=D(a)}});t.prototype.setCompare=function(a,b){this.isXAxis||(this.series.forEach(function(b){b.setCompare(a)}),E(b,!0)&&this.chart.redraw())};h.prototype.tooltipFormatter=function(a){a=a.replace("{point.change}",(0<this.change?"+":"")+c.numberFormat(this.change,
E(this.series.tooltipOptions.changeDecimals,2)));return r.apply(this,[a])};g(b,"render",function(){var a=this.chart;if(!(a.is3d&&a.is3d()||a.polar)&&this.xAxis&&!this.xAxis.isRadial){var b=this.yAxis.len;if(this.xAxis.axisLine){var c=a.plotTop+a.plotHeight-this.yAxis.pos-this.yAxis.len,d=Math.floor(this.xAxis.axisLine.strokeWidth()/2);0<=c&&(b-=Math.max(d-c,0))}!this.clipBox&&this.animate?(this.clipBox=q(a.clipBox),this.clipBox.width=this.xAxis.len,this.clipBox.height=b):a[this.sharedClipKey]&&(a[this.sharedClipKey].animate({width:this.xAxis.len,
height:b}),a[this.sharedClipKey+"m"]&&a[this.sharedClipKey+"m"].animate({width:this.xAxis.len}))}});g(m,"update",function(a){a=a.options;"scrollbar"in a&&this.navigator&&(q(!0,this.options.scrollbar,a.scrollbar),this.navigator.update({},!1),delete a.scrollbar)})});K(D,"masters/modules/stock.src.js",[],function(){});K(D,"masters/highstock.src.js",[D["masters/highcharts.src.js"]],function(c){c.product="Highstock";return c});D["masters/highstock.src.js"]._modules=D;return D["masters/highstock.src.js"]});
//# sourceMappingURL=highstock.js.map
/*
 Highstock JS v7.2.1 (2019-10-31)

 Indicator series type for Highstock

 (c) 2010-2019 Pawel Fus, Sebastian Bochan

 License: www.highcharts.com/license
*/
(function(a){"object"===typeof module&&module.exports?(a["default"]=a,module.exports=a):"function"===typeof define&&define.amd?define("highcharts/indicators/indicators",["highcharts","highcharts/modules/stock"],function(e){a(e);a.Highcharts=e;return a}):a("undefined"!==typeof Highcharts?Highcharts:void 0)})(function(a){function e(a,f,q,r){a.hasOwnProperty(f)||(a[f]=r.apply(null,q))}a=a?a._modules:{};e(a,"mixins/indicator-required.js",[a["parts/Globals.js"]],function(a){var f=a.error;return{isParentLoaded:function(a,
p,e,h,t){if(a)return h?h(a):!0;f(t||this.generateMessage(e,p));return!1},generateMessage:function(a,f){return'Error: "'+a+'" indicator type requires "'+f+'" indicator loaded before. Please read docs: https://api.highcharts.com/highstock/plotOptions.'+a}}});e(a,"indicators/indicators.src.js",[a["parts/Globals.js"],a["parts/Utilities.js"],a["mixins/indicator-required.js"]],function(a,f,e){var r=f.extend,p=f.isArray,h=f.pick,t=f.splat,u=a.error,k=a.Series,l=a.addEvent;f=a.seriesType;var v=a.seriesTypes,
m=a.seriesTypes.ohlc.prototype,q=e.generateMessage;l(a.Series,"init",function(c){c=c.options;c.useOhlcData&&"highcharts-navigator-series"!==c.id&&r(this,{pointValKey:m.pointValKey,keys:m.keys,pointArrayMap:m.pointArrayMap,toYData:m.toYData})});l(k,"afterSetOptions",function(c){c=c.options;var a=c.dataGrouping;a&&c.useOhlcData&&"highcharts-navigator-series"!==c.id&&(a.approximation="ohlc")});f("sma","line",{name:void 0,tooltip:{valueDecimals:4},linkedTo:void 0,compareToMain:!1,params:{index:0,period:14}},
{processData:function(){var a=this.options.compareToMain,d=this.linkedParent;k.prototype.processData.apply(this,arguments);d&&d.compareValue&&a&&(this.compareValue=d.compareValue)},bindTo:{series:!0,eventName:"updatedData"},hasDerivedData:!0,useCommonDataGrouping:!0,nameComponents:["period"],nameSuffixes:[],calculateOn:"init",requiredIndicators:[],requireIndicators:function(){var a={allLoaded:!0};this.requiredIndicators.forEach(function(c){v[c]?v[c].prototype.requireIndicators():(a.allLoaded=!1,a.needed=
c)});return a},init:function(a,d){function c(){var a=b.points||[],c=(b.xData||[]).length,d=b.getValues(b.linkedParent,b.options.params)||{values:[],xData:[],yData:[]},f=[],e=!0;if(c&&!b.hasGroupedData&&b.visible&&b.points)if(b.cropped){if(b.xAxis){var g=b.xAxis.min;var w=b.xAxis.max}c=b.cropData(d.xData,d.yData,g,w);for(g=0;g<c.xData.length;g++)f.push([c.xData[g]].concat(t(c.yData[g])));c=d.xData.indexOf(b.xData[0]);g=d.xData.indexOf(b.xData[b.xData.length-1]);-1===c&&g===d.xData.length-2&&f[0][0]===
a[0].x&&f.shift();b.updateData(f)}else d.xData.length!==c-1&&d.xData.length!==c+1&&(e=!1,b.updateData(d.values));e&&(b.xData=d.xData,b.yData=d.yData,b.options.data=d.values);!1===b.bindTo.series&&(delete b.processedXData,b.isDirty=!0,b.redraw());b.isDirtyData=!1}var b=this,f=b.requireIndicators();if(!f.allLoaded)return u(q(b.type,f.needed));k.prototype.init.call(b,a,d);a.linkSeries();b.dataEventsToUnbind=[];if(!b.linkedParent)return u("Series "+b.options.linkedTo+" not found! Check `linkedTo`.",!1,
a);b.dataEventsToUnbind.push(l(b.bindTo.series?b.linkedParent:b.linkedParent.xAxis,b.bindTo.eventName,c));if("init"===b.calculateOn)c();else var e=l(b.chart,b.calculateOn,function(){c();e()});return b},getName:function(){var a=this.name,d=[];a||((this.nameComponents||[]).forEach(function(a,b){d.push(this.options.params[a]+h(this.nameSuffixes[b],""))},this),a=(this.nameBase||this.type.toUpperCase())+(this.nameComponents?" ("+d.join(", ")+")":""));return a},getValues:function(a,d){var c=d.period,b=
a.xData;a=a.yData;var f=a.length,e=0,h=0,k=[],l=[],m=[],n=-1;if(b.length<c)return!1;for(p(a[0])&&(n=d.index?d.index:0);e<c-1;)h+=0>n?a[e]:a[e][n],e++;for(d=e;d<f;d++){h+=0>n?a[d]:a[d][n];var g=[b[d],h/c];k.push(g);l.push(g[0]);m.push(g[1]);h-=0>n?a[d-e]:a[d-e][n]}return{values:k,xData:l,yData:m}},destroy:function(){this.dataEventsToUnbind.forEach(function(a){a()});k.prototype.destroy.call(this)}});""});e(a,"masters/indicators/indicators.src.js",[],function(){})});
//# sourceMappingURL=indicators.js.map
/*
 Highstock JS v7.2.1 (2019-10-31)

 Indicator series type for Highstock

 (c) 2010-2019 Sebastian Bochan

 License: www.highcharts.com/license
*/
(function(a){"object"===typeof module&&module.exports?(a["default"]=a,module.exports=a):"function"===typeof define&&define.amd?define("highcharts/indicators/ema",["highcharts","highcharts/modules/stock"],function(b){a(b);a.Highcharts=b;return a}):a("undefined"!==typeof Highcharts?Highcharts:void 0)})(function(a){function b(a,f,b,g){a.hasOwnProperty(f)||(a[f]=g.apply(null,b))}a=a?a._modules:{};b(a,"indicators/ema.src.js",[a["parts/Globals.js"],a["parts/Utilities.js"]],function(a,b){var f=b.isArray;
b=a.seriesType;var g=a.correctFloat;b("ema","sma",{params:{index:3,period:9}},{accumulatePeriodPoints:function(a,l,c){for(var b=0,d=0,e;d<a;)e=0>l?c[d]:c[d][l],b+=e,d++;return b},calculateEma:function(a,b,c,h,m,e,f){a=a[c-1];b=0>e?b[c-1]:b[c-1][e];h=void 0===m?f:g(b*h+m*(1-h));return[a,h]},getValues:function(a,b){var c=b.period,h=a.xData,m=(a=a.yData)?a.length:0,e=2/(c+1),d=[],g=[],l=[],n=-1;if(m<c)return!1;f(a[0])&&(n=b.index?b.index:0);for(b=this.accumulatePeriodPoints(c,n,a)/c;c<m+1;c++){var k=
this.calculateEma(h,a,c,e,k,n,b);d.push(k);g.push(k[0]);l.push(k[1]);k=k[1]}return{values:d,xData:g,yData:l}}});""});b(a,"masters/indicators/ema.src.js",[],function(){})});
//# sourceMappingURL=ema.js.map