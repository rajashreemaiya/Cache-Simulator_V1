num_clients = 500; %number of clients (determines number of files created)

fileprefix = strcat('client_input_');
filesuffix = '.csv';

requests = 80000/num_clients;
data = randi([1,20000],1,round(requests));
i=0
for filenum=1:num_clients
    data = randi([1,20000],1,round(requests));
    y = [];
    for j=1:length(data)
        y = [y;i+1];
        i = i+1;
    end 

    x = [data',y];
    csvwrite([fileprefix num2str(num_clients) '_' num2str(filenum-1) filesuffix],x);
end


