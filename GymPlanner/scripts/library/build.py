import json, sys
from part3 import L
from collections import Counter
MUSCLES={"chest","front_delts","side_delts","rear_delts","traps","lats","upper_back","lower_back","biceps","triceps","forearms","abs","obliques","glutes","quads","hamstrings","adductors","abductors","calves","hip_flexors"}
GROUPS=["back","chest","shoulders","biceps","triceps","forearms","legs","glutes","calves","core"]
for n,d in L.items():
    assert d["group"] in GROUPS,n
    for m in d["primary"]+d["secondary"]: assert m in MUSCLES,(n,m)
    assert 3<=len(d["steps"])<=5,(n,len(d["steps"]))
    assert 1<=len(d["mistakes"])<=3,n
    assert d["equipment"]==d["equipment"].lower()
print(len(L), Counter(d["group"] for d in L.values()))
v1=['Pull-ups','Chin-ups','Wide-grip lat pulldown','Close-grip lat pulldown','Machine lat pulldown','Cable wide-grip row','Seated cable row','Single-arm row','Barbell row','T-bar row','Chest-supported row','Deadlift','Back extensions','Straight-arm pulldown','Face pull','Barbell bench press','Incline barbell bench press','Dumbbell bench press','Incline dumbbell press','Machine chest press','Cable fly','Pec deck','Dips','Push-ups','Decline bench press','Shoulder warm-ups','Reverse pec deck','Machine shoulder press','Overhead press','Seated dumbbell press','Lateral raise','Cable lateral raise','Front raise','Rear delt fly','Upright row','Shrugs','Barbell curl','EZ-bar curl','Dumbbell curl','Hammer curl','Incline dumbbell curl','Preacher curl','Cable curl','Concentration curl','Tricep rope pushdown','Tricep bar pushdown','Overhead cable triceps extension','Skull crushers','Close-grip bench press','Overhead dumbbell extension','Bench dips','Single-arm cable extension','Back squat','Front squat','Leg press','Hack squat','Romanian deadlift','Bulgarian split squat','Walking lunges','Leg extension','Lying leg curl','Seated leg curl','Hip thrust','Standing calf raise','Seated calf raise','Goblet squat','Hip abductor','Hip adductor','Plank','Hanging leg raise','Cable crunch','Ab wheel rollout','Russian twist','Crunches','Side plank','Dead bug']
missing=[n for n in v1 if n not in L]; print("v1 missing:",missing)
tmpl=["Pull-ups","Wide-grip lat pulldown","Close-grip lat pulldown","Cable wide-grip row","Back extensions","EZ-bar curl","Cable lateral raise","Barbell bench press","Incline barbell bench press","Pec deck","Decline chest fly (machine)","Tricep bar pushdown","Overhead cable triceps extension","Machine lateral raise","Reverse pec deck","Machine shoulder press","Incline dumbbell curl","Cable curl","Cable crunch","Reverse dragon flag","Leg extension","Seated leg curl","Leg press","Hack squat","Hip abductor","Hip adductor","Seated calf raise","Hip thrust"]
print("template missing:",[n for n in tmpl if n not in L])
out={"formatVersion":2,"libraryVersion":2,"exercises":[
 {"name":n,"muscleGroup":d["group"],"equipment":d["equipment"],
  "guide":{"primaryMuscles":d["primary"],"secondaryMuscles":d["secondary"],"steps":d["steps"],"mistakes":d["mistakes"],"videoQuery":n.lower()+" form"}}
 for n,d in L.items()]}
json.dump(out,open(sys.argv[1],"w"),indent=1,ensure_ascii=False)

def E(name,sets,rmin,rmax,rest,fail=False,opt=False,side=False,warm=False,note=""):
    d=L[name]
    return {"name":name,"muscleGroup":d["group"],"equipment":d["equipment"],"sets":sets,"repMin":rmin,"repMax":rmax,"toFailure":fail,"perSide":side,"restSeconds":rest,"note":note,"isOptional":opt,"isWarmup":warm,"supersetGroup":None,"dropSets":0}
plan={"formatVersion":2,"type":"plan","plan":{"name":"4-Day Split","description":"Back, Chest, Shoulders & Arms, and Leg every other week.","days":[
 {"name":"Back","note":"","frequency":"weekly","exercises":[E("Pull-ups",1,8,12,60,warm=True),E("Wide-grip lat pulldown",3,6,8,60),E("Close-grip lat pulldown",3,6,8,60),E("Cable wide-grip row",3,8,12,90,fail=True),E("Back extensions",2,8,12,90,fail=True),E("EZ-bar curl",3,8,12,90,fail=True)]},
 {"name":"Chest","note":"","frequency":"weekly","exercises":[E("Cable lateral raise",3,8,12,90,side=True),E("Barbell bench press",4,3,12,90,opt=True),E("Incline barbell bench press",3,8,12,90,fail=True),E("Pec deck",3,8,12,90,fail=True),E("Decline chest fly (machine)",3,8,12,90,fail=True),E("Tricep bar pushdown",3,8,12,90),E("Overhead cable triceps extension",3,8,12,90)]},
 {"name":"Shoulders & Arms","note":"","frequency":"weekly","exercises":[E("Cable lateral raise",3,8,12,60,side=True),E("Machine lateral raise",3,8,12,90,opt=True),E("Reverse pec deck",3,8,12,60),E("Machine shoulder press",3,8,12,90,fail=True),E("Incline dumbbell curl",3,8,12,90,fail=True),E("Tricep bar pushdown",3,8,12,90,fail=True),E("Cable curl",3,8,12,90,fail=True),E("Overhead cable triceps extension",3,8,12,90),E("Cable crunch",3,8,12,90),E("Reverse dragon flag",3,3,5,90,note="Slow on the descent")]},
 {"name":"Leg","note":"Every 2 weeks.","frequency":"biweekly","exercises":[E("Leg extension",3,8,12,90,fail=True),E("Seated leg curl",3,8,12,90,fail=True),E("Leg press",3,8,12,90),E("Hack squat",3,8,12,90),E("Hip abductor",3,8,12,90),E("Hip adductor",3,8,12,90),E("Seated calf raise",3,8,12,90,fail=True),E("Hip thrust",2,8,12,90,fail=True)]},
]}}
json.dump(plan,open(sys.argv[2],"w"),indent=2,ensure_ascii=False)
