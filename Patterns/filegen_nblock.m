% parameters for generating patterns
num_clients = 500; %number of clients (determines number of files created)

fileprefix = strcat('client_input_nb_');
filesuffix = '.csv';


num_of_patterns = 20; %total number of master patterns
min_p_length = 5; %minimum pattern length
% max_p_length = 10; %maximum pattern length
perc_patterns_all = [5 10 15]; % % of patterns in final file

% processing
requests = 80000/num_clients;
data = randi([1,20000],1,round(requests));


% replace elements with master patterns and generate files
for ii=1:length(perc_patterns_all)
    perc_patterns = perc_patterns_all(ii); %pick the percentage number from list
    
    % create master patterns
    for pat_num = 1:num_of_patterns
        max_p_length = min([12,floor(100/perc_patterns)]);
        pat_len = randi([min_p_length,max_p_length],1,1);
        master_patterns{pat_num} = [randi(20000,pat_len,1), pat_num*ones(pat_len,1)];
    end

    
    for filenum=1:num_clients
        data = randi([1,20000],1,round(requests));
        y = (1:length(data)) + num_of_patterns;
        y = y + ((filenum-1)*round(requests));
        B = [data',y'];


        r = randperm(floor((length(data)-max_p_length)/max_p_length),floor(requests*perc_patterns/100)-1)*max_p_length;     
        for i=1:floor(requests*perc_patterns/100)-1

            pat_to_insert = master_patterns(randi(numel(master_patterns)));
            pat_to_insert_mat = cell2mat(pat_to_insert);

            B(r(i):r(i)+(length(pat_to_insert_mat)-1),1:2) = pat_to_insert_mat;

        end

        perc_indicator=[num2str(perc_patterns) '_'];
        n = strcat(num2str(num_clients),'_');
        csvwrite([fileprefix perc_indicator n num2str(filenum-1) filesuffix],B);
    end

end