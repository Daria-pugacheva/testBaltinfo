import java.io.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class MainApp {

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        //вычитываем данные из архива в обычный текстовый файл
        Path source = Paths.get("lng-4.txt.gz");
        Path target = Paths.get("lng-4.txt");

        if (Files.notExists(source)) {
            System.err.printf("Файл %s не существует", source);
            return;
        }
        try {
            decompressGzip(source, target);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //собираем строки из файла Lng-4.txt в коллекцию уникалных строк
        HashSet<String> allUniqueStrings = new HashSet<>();
        try {
            File file = new File("lng-4.txt");
            FileReader fr = new FileReader(file);
            BufferedReader reader = new BufferedReader(fr);
            String line = reader.readLine();
            while (line != null) {
                allUniqueStrings.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Время вычитки файла из архива + формирования сета уникальных строк "
                + (System.currentTimeMillis() - start));
        //делаем из коллекции строк коллекцию ArrayList-ов,в которых лежат части строки
        List<ArrayList<String>> allStringsAsArrays = stringCollectionToArrayOfSubstringCollection(allUniqueStrings);
        System.out.println("Время до момента формировани коллекции листов с элементами строк "
                + (System.currentTimeMillis() - start));
        //формируем группы строк, в которых совпадают элементы, находящиеся на одной позиции
        List<List<String>> readyGroups = stringsJoinedInGroups(allStringsAsArrays);
        System.out.println("Время до формирования групп  "
                + (System.currentTimeMillis() - start));
        // в коллекции групп сортируем группы по количеству строк в них (по кол-ву элементов)
        List<List<String>> sortedLargeGroups = sortGroupsBySize(readyGroups);
        System.out.println("Время до сортировки групп "
                + (System.currentTimeMillis() - start));
        //Выводим группы в файл в заданном формате:
        exportToFile(sortedLargeGroups);

        System.out.println((System.currentTimeMillis() - start));

    }

    //вычитываем содержимое файла из архива
   private static void decompressGzip(Path source, Path target) throws IOException {
        try (GZIPInputStream gis = new GZIPInputStream(
                new FileInputStream(source.toFile()));
             FileOutputStream fos = new FileOutputStream(target.toFile())) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    //делает из коллекции строк коллекцию ArrayList-ов,в которых лежат части строки
    private static List<ArrayList<String>> stringCollectionToArrayOfSubstringCollection(HashSet<String> set) {
        List<ArrayList<String>> allStringsAsArrays = new ArrayList<>();
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String str = iterator.next();
            String[] array = str.split(";");
            allStringsAsArrays.add(new ArrayList<>(Arrays.asList(array)));
        }
        //System.out.println("В коллекции уникальных строк " + allStringsAsArrays.size() + " элементов");
        return allStringsAsArrays;
    }

    private static List<List<String>> stringsJoinedInGroups(List<ArrayList<String>> list) {
        //группируем коллекции в по принципу совпадения элементов находящихся на одних и тех же позициях
        // (сравнение только одной коллекции со всеми следующими последовательно)
        List<List<String>> largeGroups = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            List<String> currentArray = list.get(i);
            List<String> group = new ArrayList<>();
            group.add(listToString(currentArray));
//            System.out.println("Итерация групп i" + i + "ЧТОБЫ ЗАМЕНТНО БЫЛО ТЕКСТ ");
            for (int j = i + 1; j < list.size(); j++) {
                List<String> nextArray = list.get(j);
//                System.out.println("Итерация групп j" + j);
                int limitForCompare = Math.min(currentArray.size(), nextArray.size());
                for (int k = 0; k < limitForCompare; k++) {
                    //  System.out.println("Итерация групп k до добавления подходящих" + k);
                    if (currentArray.get(k).length() > 2 && currentArray.get(k).equals(nextArray.get(k))) {
                        group.add(listToString(nextArray));
                        //   System.out.println("Добавали подходящую группу на k" + k);
                        break;
                    }
                }
            }
            if (group.size() > 1) {
                largeGroups.add(group);
                // System.out.println("Добавили группу к списку больших групп на итерации i" + i);
            }
        }

        //объединяем получившиеся группы по принципу, что в них есть совпадающие строки
        // System.out.println("Переходим ко второй стадии объединения");
        List<Integer> indexesToRemove = new ArrayList<>();
        for (int i = 0; i < largeGroups.size(); i++) {
            List<String> currentList = largeGroups.get(i);
            // System.out.println("Вторая Итерация групп i" + i);
            for (int j = i + 1; j < largeGroups.size(); j++) {
                List<String> nextList = largeGroups.get(j);
                //   System.out.println("Вторая Итерация групп j" + j);
                for (int k = 0; k < currentList.size(); k++) {
                    //       System.out.println("Вторая Итерация групп k" + k);
                    boolean check = false;
                    for (int l = 0; l < nextList.size(); l++) {
                        //         System.out.println("Вторая Итерация групп l" + l);
                        if (currentList.get(k).equals(nextList.get(l))) {
                            currentList.addAll(nextList);
                            indexesToRemove.add(j);
                            check = true;
                            break;
                        }
                    }
                    if (check) {
                        //        System.out.println("Выход из второй Итерации");
                        break;
                    }
                }
            }
            //удаляем те коллекции, которые мы объединили с текущей проверяемой на совпадения
            for (int j = 0; j < indexesToRemove.size(); j++) {
                largeGroups.remove(indexesToRemove.get(j));
                //       System.out.println("Удаляем лишние коллекции после объединения");
            }
            indexesToRemove.clear();
        }
        return largeGroups;
    }

    private static String listToString(List<String> source) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < source.size() - 1; i++) {
            stringBuilder.append(source.get(i) + ";");
        }
        stringBuilder.append(source.get(source.size() - 1));
        return stringBuilder.toString();
    }

    private static int maxSizeOfGroup(List<List<String>> listOfGroups) {
        int maxSize = 2;
        for (int i = 0; i < listOfGroups.size(); i++) {
            if (listOfGroups.get(i).size() > maxSize) {
                maxSize = listOfGroups.get(i).size();
            }
        }
        return maxSize;
    }


    // в коллекции групп сортируем группы по количеству строк в них (по кол-ву элементов)
    private static List<List<String>> sortGroupsBySize(List<List<String>> source) {
        int maxSizeOfGroup = maxSizeOfGroup(source);
        List<List<String>> sortedLargeGroups = new ArrayList<>();
        while (maxSizeOfGroup > 1) {
            for (int i = 0; i < source.size(); i++) {
                if (source.get(i).size() == maxSizeOfGroup) {
                    sortedLargeGroups.add(source.get(i));
                }
            }
            maxSizeOfGroup--;
        }
        return sortedLargeGroups;
    }

    //записываем сгруппированные строки в требуемом формате в новый файл
    private static void exportToFile(List<List<String>> list) {
        try (FileOutputStream out = new FileOutputStream("groups.txt")) {
            out.write(("Число групп с более, чем одним элементом " + (list.size()))
                    .getBytes(StandardCharsets.UTF_8));
            for (int i = 0; i < list.size(); i++) {
                List<String> groupForFile = list.get(i);
                out.write(("Группа " + (i + 1)).getBytes(StandardCharsets.UTF_8));
                for (int j = 0; j < groupForFile.size(); j++) {
                    out.write(groupForFile.get(j).getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}





