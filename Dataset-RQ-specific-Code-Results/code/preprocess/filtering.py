import re
import os

selected_features = ["Age",
                     "SLOCStandard",
                     "Readability",
                     "SimpleReadability",
                     "NVAR",
                     "NCOMP",
                     "Mcclure",
                     "McCabe",
                     "IndentSTD",
                     "MaximumBlockDepth",
                     "totalFanOut",
                     "Length",
                     "MaintainabilityIndex",
                     "SATD",
                     "Parameters",
                     "LocalVariables",
                     "ChangeAtMethodAge",
                     "NewAdditions",
                     "DiffSizes",
                     "EditDistances",
                     "CriticalEditDistances",
                     "TangledWMoveandFileRename",
                     "Buggycommiit",
                     "PotentiallyBuggycommit",
                     "RiskyCommit",
                     "Effort", 
                     "Time", 
                     "Volume", 
                     "HalsteadBugs", 
                     "Difficulty",
                     "file"
                     ]

def filter(src_path, dest_path):

  for file in os.listdir(src_path):
    indexes = find_index(os.path.join(src_path, file))
    fr = open(os.path.join(src_path, file),"r", encoding="utf8")
    line = fr.readline()
    lines = fr.readlines()
    fr.close()
    fw = open(os.path.join(dest_path, file),"w")

    # Writing the header with selected features

    for indx, feature in enumerate(selected_features):
      fw.write(feature)
      if indx != len(selected_features) - 1:
        fw.write('\t')
    fw.write("\n")

    c = 0 # number of problems

    for line in lines:
      line = line.strip()
      problem =  check_problem(line, indexes) 
      if problem == 1:
          c+=1
          continue
      data = line.strip().split("\t")
      for indx, feature in enumerate(selected_features):
        fw.write(data[indexes[feature]])
        if indx != len(selected_features) - 1:
          fw.write('\t')
      fw.write("\n")  

    print (file, len(lines)-1, c)
    fw.flush()
    fw.close()

def find_index(file_path):
  indexes = {}
  fr = open(file_path, "r", encoding="utf8")
  line = fr.readline()  # headerfeature_index
  line = line.strip()
  data = line.split("\t")
  for i in range(len(data)):
    indexes[data[i]] = i
  return indexes

def check_problem(line, indexes):
  line = line.strip()
  data = line.split("\t")
  age = int(data[indexes["Age"]])
  if age < 0:
    return 1
  dates =  data[indexes["ChangeAtMethodAge"]]
  dates = dates.split("#")
  prev = 0
  for d in dates:
    d = int(float(d))
    if d < 0:
      return 1
    if d < prev:
      return 1
    prev = d 
  
  return 0

if __name__ == "__main__":
  folders = ["ImpactedByMocking", "MockedMethodMocked", "UnmockedMethod"]
  src_path="/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/uncleaned"
  dest_path="/Users/mumtahinaahmed/Desktop/Mockito/Mockito-2025/data/cleaned"

  for folder in folders:
    src_folder_path = os.path.join(src_path, folder)
    dest_folder_path = os.path.join(dest_path, folder)
    if not os.path.exists(dest_folder_path):
      os.makedirs(dest_folder_path)
    filter(src_folder_path, dest_folder_path)