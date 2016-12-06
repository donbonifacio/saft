(ns saft.countries)

(def countries 
  [{:name "Portugal" :code "PT" :phone-code "351"}
   {:name "Afghanistan" :code "AF" :phone-code "93"}
   {:name "Albania" :code "AL" :phone-code "355"}
   {:name "Algeria" :code "DZ" :phone-code "213"}
   {:name "American Samoa" :code "AS" :phone-code "1-684"}
   {:name "Andorra" :code "AD" :phone-code "376"}
   {:name "Angola" :code "AO" :phone-code "244"}
   {:name "Anguilla" :code "AI" :phone-code "1-264"}
   {:name "Antarctica" :code "AQ" :phone-code "672"}
   {:name "Antigua and Barbuda" :code "AG" :phone-code "1-268"}
   {:name "Argentina" :code "AR" :phone-code "54"}
   {:name "Armenia" :code "AM" :phone-code "374"}
   {:name "Aruba" :code "AW" :phone-code "297"}
   {:name "Australia" :code "AU" :phone-code "61"}
   {:name "Austria" :code "AT" :phone-code "43"}
   {:name "Azerbaijan" :code "AZ" :phone-code "994"}
   {:name "Bahamas" :code "BS" :phone-code "1-242"}
   {:name "Bahrain" :code "BH" :phone-code "973"}
   {:name "Bangladesh" :code "BD" :phone-code "880"}
   {:name "Barbados" :code "BB" :phone-code "1-246"}
   {:name "Belarus" :code "BY" :phone-code "375"}
   {:name "Belgium" :code "BE" :phone-code "32"}
   {:name "Belize" :code "BZ" :phone-code "501"}
   {:name "Benin" :code "BJ" :phone-code "229"}
   {:name "Bermuda" :code "BM" :phone-code "1-441"}
   {:name "Bhutan" :code "BT" :phone-code "975"}
   {:name "Bolivia" :code "BO" :phone-code "591"}
   {:name "Bosnia and Herzegovina" :code "BA" :phone-code "387"}
   {:name "Botswana" :code "BW" :phone-code "267"}
   {:name "Brazil" :code "BR" :phone-code "55"}
   {:name "British Indian Ocean Territory" :code "IO" :phone-code "246"}
   {:name "British Virgin Islands" :code "VG" :phone-code "1-284"}
   {:name "Brunei" :code "BN" :phone-code "673"}
   {:name "Bulgaria" :code "BG" :phone-code "359"}
   {:name "Burkina Faso" :code "BF" :phone-code "226"}
   {:name "Burundi" :code "BI" :phone-code "257"}
   {:name "Cambodia" :code "KH" :phone-code "855"}
   {:name "Cameroon" :code "CM" :phone-code "237"}
   {:name "Canada" :code "CA" :phone-code "1"}
   {:name "Cape Verde" :code "CV" :phone-code "238"}
   {:name "Cayman Islands" :code "KY" :phone-code "1-345"}
   {:name "Central African Republic" :code "CF" :phone-code "236"}
   {:name "Chad" :code "TD" :phone-code "235"}
   {:name "Chile" :code "CL" :phone-code "56"}
   {:name "China" :code "CN" :phone-code "86"}
   {:name "Christmas Island" :code "CX" :phone-code "61"}
   {:name "Cocos Islands" :code "CC" :phone-code "61"}
   {:name "Colombia" :code "CO" :phone-code "57"}
   {:name "Comoros" :code "KM" :phone-code "269"}
   {:name "Cook Islands" :code "CK" :phone-code "682"}
   {:name "Costa Rica" :code "CR" :phone-code "506"}
   {:name "Croatia" :code "HR" :phone-code "385"}
   {:name "Cuba" :code "CU" :phone-code "53"}
   {:name "Curacao" :code "CW" :phone-code "599"}
   {:name "Cyprus" :code "CY" :phone-code "357"}
   {:name "Czech Republic" :code "CZ" :phone-code "420"}
   {:name "Democratic Republic of the Congo" :code "CD" :phone-code "243"}
   {:name "Denmark" :code "DK" :phone-code "45"}
   {:name "Djibouti" :code "DJ" :phone-code "253"}
   {:name "Dominica" :code "DM" :phone-code "1-767"}
   {:name "Dominican Republic" :code "DO" :phone-code "1-809, 1-829, 1-849"}
   {:name "East Timor" :code "TL" :phone-code "670"}
   {:name "Ecuador" :code "EC" :phone-code "593"}
   {:name "Egypt" :code "EG" :phone-code "20"}
   {:name "El Salvador" :code "SV" :phone-code "503"}
   {:name "Equatorial Guinea" :code "GQ" :phone-code "240"}
   {:name "Eritrea" :code "ER" :phone-code "291"}
   {:name "Estonia" :code "EE" :phone-code "372"}
   {:name "Ethiopia" :code "ET" :phone-code "251"}
   {:name "Falkland Islands" :code "FK" :phone-code "500"}
   {:name "Faroe Islands" :code "FO" :phone-code "298"}
   {:name "Fiji" :code "FJ" :phone-code "679"}
   {:name "Finland" :code "FI" :phone-code "358"}
   {:name "France" :code "FR" :phone-code "33"}
   {:name "French Polynesia" :code "PF" :phone-code "689"}
   {:name "Gabon" :code "GA" :phone-code "241"}
   {:name "Gambia" :code "GM" :phone-code "220"}
   {:name "Georgia" :code "GE" :phone-code "995"}
   {:name "Germany" :code "DE" :phone-code "49"}
   {:name "Ghana" :code "GH" :phone-code "233"}
   {:name "Gibraltar" :code "GI" :phone-code "350"}
   {:name "Greece" :code "GR" :phone-code "30"}
   {:name "Greenland" :code "GL" :phone-code "299"}
   {:name "Grenada" :code "GD" :phone-code "1-473"}
   {:name "Guam" :code "GU" :phone-code "1-671"}
   {:name "Guatemala" :code "GT" :phone-code "502"}
   {:name "Guernsey" :code "GG" :phone-code "44-1481"}
   {:name "Guinea" :code "GN" :phone-code "224"}
   {:name "Guinea-Bissau" :code "GW" :phone-code "245"}
   {:name "Guyana" :code "GY" :phone-code "592"}
   {:name "Haiti" :code "HT" :phone-code "509"}
   {:name "Honduras" :code "HN" :phone-code "504"}
   {:name "Hong Kong" :code "HK" :phone-code "852"}
   {:name "Hungary" :code "HU" :phone-code "36"}
   {:name "Iceland" :code "IS" :phone-code "354"}
   {:name "India" :code "IN" :phone-code "91"}
   {:name "Indonesia" :code "ID" :phone-code "62"}
   {:name "Iran" :code "IR" :phone-code "98"}
   {:name "Iraq" :code "IQ" :phone-code "964"}
   {:name "Ireland" :code "IE" :phone-code "353"}
   {:name "Isle of Man" :code "IM" :phone-code "44-1624"}
  {:name "Israel" :code "IL" :phone-code "972"}
  {:name "Italy" :code "IT" :phone-code "39"}
  {:name "Ivory Coast" :code "CI" :phone-code "225"}
  {:name "Jamaica" :code "JM" :phone-code "1-876"}
  {:name "Japan" :code "JP" :phone-code "81"}
  {:name "Jersey" :code "JE" :phone-code "44-1534"}
  {:name "Jordan" :code "JO" :phone-code "962"}
  {:name "Kazakhstan" :code "KZ" :phone-code "7"}
  {:name "Kenya" :code "KE" :phone-code "254"}
  {:name "Kiribati" :code "KI" :phone-code "686"}
  {:name "Kosovo" :code "XK" :phone-code "383"}
  {:name "Kuwait" :code "KW" :phone-code "965"}
  {:name "Kyrgyzstan" :code "KG" :phone-code "996"}
  {:name "Laos" :code "LA" :phone-code "856"}
  {:name "Latvia" :code "LV" :phone-code "371"}
  {:name "Lebanon" :code "LB" :phone-code "961"}
  {:name "Lesotho" :code "LS" :phone-code "266"}
  {:name "Liberia" :code "LR" :phone-code "231"}
  {:name "Libya" :code "LY" :phone-code "218"}
  {:name "Liechtenstein" :code "LI" :phone-code "423"}
  {:name "Lithuania" :code "LT" :phone-code "370"}
  {:name "Luxembourg" :code "LU" :phone-code "352"}
  {:name "Macao" :code "MO" :phone-code "853"}
  {:name "Macedonia" :code "MK" :phone-code "389"}
  {:name "Madagascar" :code "MG" :phone-code "261"}
  {:name "Malawi" :code "MW" :phone-code "265"}
  {:name "Malaysia" :code "MY" :phone-code "60"}
  {:name "Maldives" :code "MV" :phone-code "960"}
  {:name "Mali" :code "ML" :phone-code "223"}
  {:name "Malta" :code "MT" :phone-code "356"}
  {:name "Marshall Islands" :code "MH" :phone-code "692"}
  {:name "Mauritania" :code "MR" :phone-code "222"}
  {:name "Mauritius" :code "MU" :phone-code "230"}
  {:name "Mayotte" :code "YT" :phone-code "262"}
  {:name "Mexico" :code "MX" :phone-code "52"}
  {:name "Micronesia" :code "FM" :phone-code "691"}
  {:name "Moldova" :code "MD" :phone-code "373"}
  {:name "Monaco" :code "MC" :phone-code "377"}
  {:name "Mongolia" :code "MN" :phone-code "976"}
  {:name "Montenegro" :code "ME" :phone-code "382"}
  {:name "Montserrat" :code "MS" :phone-code "1-664"}
  {:name "Morocco" :code "MA" :phone-code "212"}
  {:name "Mozambique" :code "MZ" :phone-code "258"}
  {:name "Myanmar" :code "MM" :phone-code "95"}
  {:name "Namibia" :code "NA" :phone-code "264"}
  {:name "Nauru" :code "NR" :phone-code "674"}
  {:name "Nepal" :code "NP" :phone-code "977"}
  {:name "Netherlands" :code "NL" :phone-code "31"}
  {:name "Netherlands Antilles" :code "AN" :phone-code "599"}
  {:name "New Caledonia" :code "NC" :phone-code "687"}
  {:name "New Zealand" :code "NZ" :phone-code "64"}
  {:name "Nicaragua" :code "NI" :phone-code "505"}
  {:name "Niger" :code "NE" :phone-code "227"}
  {:name "Nigeria" :code "NG" :phone-code "234"}
  {:name "Niue" :code "NU" :phone-code "683"}
  {:name "North Korea" :code "KP" :phone-code "850"}
  {:name "Northern Mariana Islands" :code "MP" :phone-code "1-670"}
  {:name "Norway" :code "NO" :phone-code "47"}
  {:name "Oman" :code "OM" :phone-code "968"}
  {:name "Pakistan" :code "PK" :phone-code "92"}
  {:name "Palau" :code "PW" :phone-code "680"}
  {:name "Palestine" :code "PS" :phone-code "970"}
  {:name "Panama" :code "PA" :phone-code "507"}
  {:name "Papua New Guinea" :code "PG" :phone-code "675"}
  {:name "Paraguay" :code "PY" :phone-code "595"}
  {:name "Peru" :code "PE" :phone-code "51"}
  {:name "Philippines" :code "PH" :phone-code "63"}
  {:name "Pitcairn" :code "PN" :phone-code "64"}
  {:name "Poland" :code "PL" :phone-code "48"}
  {:name "Puerto Rico" :code "PR" :phone-code "1-787, 1-939"}
  {:name "Qatar" :code "QA" :phone-code "974"}
  {:name "Republic of the Congo" :code "CG" :phone-code "242"}
  {:name "Reunion" :code "RE" :phone-code "262"}
  {:name "Romania" :code "RO" :phone-code "40"}
  {:name "Russia" :code "RU" :phone-code "7"}
  {:name "Rwanda" :code "RW" :phone-code "250"}
  {:name "Saint Barthelemy" :code "BL" :phone-code "590"}
  {:name "Saint Helena" :code "SH" :phone-code "290"}
  {:name "Saint Kitts and Nevis" :code "KN" :phone-code "1-869"}
  {:name "Saint Lucia" :code "LC" :phone-code "1-758"}
  {:name "Saint Martin" :code "MF" :phone-code "590"}
  {:name "Saint Pierre and Miquelon" :code "PM" :phone-code "508"}
  {:name "Saint Vincent and the Grenadines" :code "VC" :phone-code "1-784"}
  {:name "Samoa" :code "WS" :phone-code "685"}
  {:name "San Marino" :code "SM" :phone-code "378"}
  {:name "Sao Tome and Principe" :code "ST" :phone-code "239"}
  {:name "Saudi Arabia" :code "SA" :phone-code "966"}
  {:name "Senegal" :code "SN" :phone-code "221"}
  {:name "Serbia" :code "RS" :phone-code "381"}
  {:name "Seychelles" :code "SC" :phone-code "248"}
  {:name "Sierra Leone" :code "SL" :phone-code "232"}
  {:name "Singapore" :code "SG" :phone-code "65"}
  {:name "Sint Maarten" :code "SX" :phone-code "1-721"}
  {:name "Slovakia" :code "SK" :phone-code "421"}
  {:name "Slovenia" :code "SI" :phone-code "386"}
  {:name "Solomon Islands" :code "SB" :phone-code "677"}
  {:name "Somalia" :code "SO" :phone-code "252"}
  {:name "South Africa" :code "ZA" :phone-code "27"}
  {:name "South Korea" :code "KR" :phone-code "82"}
  {:name "South Sudan" :code "SS" :phone-code "211"}
  {:name "Spain" :code "ES" :phone-code "34"}
  {:name "Sri Lanka" :code "LK" :phone-code "94"}
  {:name "Sudan" :code "SD" :phone-code "249"}
  {:name "Suriname" :code "SR" :phone-code "597"}
  {:name "Svalbard and Jan Mayen" :code "SJ" :phone-code "47"}
  {:name "Swaziland" :code "SZ" :phone-code "268"}
  {:name "Sweden" :code "SE" :phone-code "46"}
  {:name "Switzerland" :code "CH" :phone-code "41"}
  {:name "Syria" :code "SY" :phone-code "963"}
  {:name "Taiwan" :code "TW" :phone-code "886"}
  {:name "Tajikistan" :code "TJ" :phone-code "992"}
  {:name "Tanzania" :code "TZ" :phone-code "255"}
  {:name "Thailand" :code "TH" :phone-code "66"}
  {:name "Togo" :code "TG" :phone-code "228"}
  {:name "Tokelau" :code "TK" :phone-code "690"}
  {:name "Tonga" :code "TO" :phone-code "676"}
  {:name "Trinidad and Tobago" :code "TT" :phone-code "1-868"}
  {:name "Tunisia" :code "TN" :phone-code "216"}
  {:name "Turkey" :code "TR" :phone-code "90"}
  {:name "Turkmenistan" :code "TM" :phone-code "993"}
  {:name "Turks and Caicos Islands" :code "TC" :phone-code "1-649"}
  {:name "Tuvalu" :code "TV" :phone-code "688"}
  {:name "U.S. Virgin Islands" :code "VI" :phone-code "1-340"}
  {:name "Uganda" :code "UG" :phone-code "256"}
  {:name "Ukraine" :code "UA" :phone-code "380"}
  {:name "United Arab Emirates" :code "AE" :phone-code "971"}
  {:name "United Kingdom" :code "GB" :phone-code "44"}
  {:name "United States" :code "US" :phone-code "1"}
  {:name "Uruguay" :code "UY" :phone-code "598"}
  {:name "Uzbekistan" :code "UZ" :phone-code "998"}
  {:name "Vanuatu" :code "VU" :phone-code "678"}
  {:name "Vatican" :code "VA" :phone-code "379"}
  {:name "Venezuela" :code "VE" :phone-code "58"}
  {:name "Vietnam" :code "VN" :phone-code "84"}
  {:name "Wallis and Futuna" :code "WF" :phone-code "681"}
  {:name "Western Sahara" :code "EH" :phone-code "212"}
  {:name "Yemen" :code "YE" :phone-code "967"}
  {:name "Zambia" :code "ZM" :phone-code "260"}
  {:name "Zimbabwe" :code "ZW" :phone-code "263"}])

(defn find-by-name [country-name]
  (first (filter #(= country-name (:name %)) countries)))

(defn country-code [country-name]
  (or (:code (find-by-name country-name))
      "PT"))
